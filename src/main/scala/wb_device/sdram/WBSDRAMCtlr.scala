package wb_device.sdram

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import wishbone.Wishbone

class SDRAM(ra: Int = 13, ba: Int = 2, dq: Int = 16) extends Bundle {
  val addr = Output(UInt(ra.W))
  val bank = Output(UInt(ba.W))
  val dataOut = Output(UInt(dq.W))
  val dqm = Output(UInt((dq/8).W))
  val clk = Output(Bool())
  val cke = Output(Bool())
  val cs_n = Output(Bool())
  val ras_n = Output(Bool())
  val cas_n = Output(Bool())
  val we_n = Output(Bool())
  val output_en = Output(Bool())
}

class WBRequest(aw: Int = 32, dw: Int = 32) extends Bundle {
  val addr = UInt(aw.W)
  val data = UInt(dw.W)
  val sel = UInt((dw/8).W)
  val we = Bool()
}

class WBSDRAMCtlr(clockSpeedHz: Long = 50000000, wbDataWidth: Int = 32,
                  ra: Int = 13, ca: Int = 10, ba: Int = 2, dq: Int = 16, cas: Int = 2) extends Module {
  require(wbDataWidth >= dq && wbDataWidth % dq == 0) // LOL Requirement
  require(cas == 2 || cas == 3, "CAS must be 2 or 3")
  // Constants
  // time are all in s
  val resetTime = 0.0002D;
  val resetClks = (clockSpeedHz * resetTime).toInt
  println(s"calculated reset clocks $resetClks")
  val numBanks = Math.pow(2, ba).toInt
  println(s"num banks = $numBanks")
  val refreshTimeoutReload = (clockSpeedHz * 0.064).toInt
  val refreshCounterReload = 8192

  // Unit s
  val tRCSsec = 0.000000015D
  val tRASsec = 0.0001D
  // Unit: clocks
  val tRP = 3 // Pre Charge
  val tMRD = 3 // Mode Register Program Time
  val tRC = 8 // AUTO Refresh Time
  val tRCD = math.max(2, math.ceil(tRCSsec * clockSpeedHz).toInt)
  val tRAS = (tRASsec * clockSpeedHz).toInt
  val tRASThreshold = 50
  println(s"tRCD = $tRCD, tRAS = $tRAS")
  val forceRefreshDeadline = refreshCounterReload * (tRC + 2)

  val dataBufferSizeInWords = wbDataWidth / dq
  val burstSize = wbDataWidth / dq
  require(burstSize == 1 || burstSize == 2 || burstSize == 4 || burstSize == 8)
  val burstLengthModeCfgValue = burstSize match {
    case 1 => "b000".U(3.W)
    case 2 => "b001".U(3.W)
    case 4 => "b010".U(3.W)
    case 8 => "b011".U(3.W)
    case _ =>  { require(false); "b001".U(3.W) }
  }

  // Begin Hardware
  val io = IO(new Bundle {
    val sdram = new SDRAM(ra, ba, dq)
    val dataIn = Input(UInt(dq.W))
    val wb = new Wishbone()
  })
  io.sdram := DontCare
  io.sdram.cke := true.B
  io.sdram.clk := clock.asBool()
  io.sdram.output_en := false.B

  io.wb.error := false.B
  io.wb.ack := false.B
  io.wb.miso_data := 0.U

  object SDRamState extends ChiselEnum {
    val idle = Value(0.U)
    val reset, reset_precharge_all, reset_set_mode, reset_auto_refresh, refresh = Value
    val row_activate, precharge = Value
    val issueRead, readWait, readLoad, readAck = Value
    val issueWrite, writeData = Value
  }
  val delayCounter = RegInit(resetClks.U(24.W))

  val refreshTimeout = RegInit(refreshTimeoutReload.U(32.W))
  val refreshCounter = RegInit(refreshCounterReload.U(16.W))
  val forceRefreshDue = Wire(Bool())
  forceRefreshDue := refreshTimeout <= forceRefreshDeadline.U && refreshCounter > 0.U

  val wbReqest = RegInit(0.U.asTypeOf(Valid(new WBRequest)))
  val dataBuffer = RegInit(VecInit(Seq.fill(dataBufferSizeInWords)(0.U(dq.W))))

  val bankActivation = RegInit(VecInit(Seq.fill(numBanks)(0.U.asTypeOf(Valid(UInt(ra.W))))))
  val activationCounter = RegInit(VecInit(Seq.fill(numBanks)(0.U(16.W))))

  // Address Mapping
  val raStart = ca-2+ba+ra
  val raEnd = ca-2+ba+1
  val caStart = ca-2
  val caEnd = 0
  val baStart = ca-2+ba
  val baEnd = ca-2+1

  val rowAddr = Wire(UInt(ra.W))
  val n_rowAddr = Wire(UInt(ra.W))
  n_rowAddr := io.wb.addr(ca-2+ba+ra,ca-2+ba+1)
  rowAddr := wbReqest.bits.addr(ca-2+ba+ra,ca-2+ba+1)
  val colAddr = Wire(UInt(ca.W))
  colAddr := Cat(wbReqest.bits.addr(ca-2, 0), 0.U(1.W))
  val bAddr = Wire(UInt(ba.W))
  val n_bAddr = Wire(UInt(ba.W))
  n_bAddr := io.wb.addr(ca-2+ba, ca-2+1)
  bAddr := wbReqest.bits.addr(ca-2+ba, ca-2+1)
  println(s"rowAddr = wbAddr[$raStart:$raEnd], ba = wbAddr[$baStart:$baEnd], colAddr = {wbAddr[$caStart:$caEnd], 1'b0}")

  val state = RegInit(SDRamState.reset)

  when(refreshTimeout === 0.U || refreshCounter === 0.U) {
    refreshTimeout := refreshTimeoutReload.U
    refreshCounter := refreshCounterReload.U
  }.otherwise {
    refreshTimeout := refreshTimeout - 1.U
  }

  def doRefresh(): Unit = {
    delayCounter := (tRC - 1).U
    state := SDRamState.refresh
    refreshCounter := refreshCounter - 1.U
  }

  // Activation Counter
  for(x <- 0 until numBanks) {
    when(bankActivation(x).valid) {
      activationCounter(x) := activationCounter(x) - 1.U
    }
  }

  def activateBank(bank: UInt, rowAddr: UInt): Unit = {
    bankActivation(bank).valid := true.B
    bankActivation(bank).bits := rowAddr
    activationCounter(bank) := (tRAS-1).U
  }
  // End Activation Counter

  val prechargeDue = Wire(Vec(numBanks, Bool()))
  for(x <- 0 until numBanks) {
    prechargeDue(x) := bankActivation(x).valid && (activationCounter(x) <= tRASThreshold.U)
  }
  val prechargeBank = Reg(UInt(ba.W))

  io.wb.stall := ((state =/= SDRamState.idle) || forceRefreshDue || prechargeDue.asUInt().orR())
  delayCounter := delayCounter - 1.U

  // Actions
  def doPrecharge(): Unit = {
    state := SDRamState.precharge
    delayCounter := (tRP - 1).U
  }
  // END Actions

  switch(state) {
    is(SDRamState.reset) {
      when(delayCounter === 0.U) {
        state := SDRamState.reset_precharge_all
        delayCounter := (tRP - 1).U
      }
    }
    is(SDRamState.reset_precharge_all) {
      when(delayCounter === 0.U) {
        state := SDRamState.reset_set_mode
        delayCounter := (tMRD - 1).U
      }
    }
    is(SDRamState.reset_set_mode) {
      when(delayCounter === 0.U) {
        state := SDRamState.reset_auto_refresh
        delayCounter := (tRC - 1).U
      }
    }
    is(SDRamState.reset_auto_refresh) {
      when(delayCounter === 0.U) {
        state := SDRamState.refresh
        delayCounter := (tRC - 1).U
      }
    }
    is(SDRamState.refresh) {
      when(delayCounter === 0.U) {
        state := SDRamState.idle
      }
    }
    is(SDRamState.idle) {
      wbReqest.valid := false.B
      when(forceRefreshDue) {
        doRefresh()
      }.elsewhen(prechargeDue.asUInt().orR()){
        for (x <- 0 until numBanks) {
          when(prechargeDue(x)) {
            prechargeBank := x.U
          }
        }
        doPrecharge()
      }.elsewhen(io.wb.cyc && io.wb.stb) {
        wbReqest.valid := true.B
        wbReqest.bits.sel := io.wb.sel
        wbReqest.bits.data := io.wb.mosi_data
        wbReqest.bits.addr := io.wb.addr
        wbReqest.bits.we := io.wb.we

        when(bankActivation(n_bAddr).valid) {
          when(bankActivation(n_bAddr).bits === n_rowAddr) {
            when (io.wb.we) {
              state := SDRamState.issueWrite
            }.otherwise {
              state := SDRamState.issueRead
            }
          }
          .otherwise {
            prechargeBank := n_bAddr
            doPrecharge()
          }
        }.otherwise {
          state := SDRamState.row_activate
          delayCounter := (tRCD - 1).U
        }
      }
    }
    is(SDRamState.precharge) {
      when(delayCounter === 0.U) {
        when(wbReqest.valid) {
          state := SDRamState.row_activate
        }.otherwise {
          state := SDRamState.idle
        }
        delayCounter := (tRCD - 1).U
        bankActivation(bAddr).valid := false.B
      }
    }
    is(SDRamState.row_activate) {
      when(delayCounter === 0.U) {
        bankActivation(n_bAddr).valid := true.B
        bankActivation(n_bAddr).bits := rowAddr
        activationCounter(n_bAddr) := tRAS.U
        when(!wbReqest.bits.we) {
          state := SDRamState.issueRead
        }.otherwise {
          state := SDRamState.issueWrite
        }
      }
    }
    is(SDRamState.issueRead) {
      wbReqest.valid := false.B
      state := SDRamState.readWait
      delayCounter := (cas - 2).U
    }
    is(SDRamState.readWait) {
      when (delayCounter === 0.U) {
        state := SDRamState.readLoad
        delayCounter := (burstSize-1).U
      }
    }
    is(SDRamState.readLoad) {
      when (delayCounter === 0.U) {
        state := SDRamState.readAck
      }
    }
    is(SDRamState.readAck) {
      io.wb.ack := io.wb.cyc
      io.wb.miso_data := dataBuffer.asUInt()
      state := SDRamState.idle
    }
    is(SDRamState.issueWrite) {
      wbReqest.valid := false.B
      if (burstSize > 1) {
        state := SDRamState.writeData
        delayCounter := ((burstSize - 1) - 1).U
      } else {
        state := SDRamState.idle
        io.wb.ack := io.wb.cyc
      }
    }
    is(SDRamState.writeData) {
      when(delayCounter === 0.U) {
        state := SDRamState.idle
        io.wb.ack := io.wb.cyc
      }
    }
  }

  io.sdram.dqm := Fill(dq/8, 0.U(1.W))
  io.sdram.cs_n := true.B
  switch(state) {
    is(SDRamState.reset) {
      io.sdram.dqm := Fill(dq/8, 1.U(1.W))
      io.sdram.cs_n := true.B
      io.sdram.ras_n := true.B
      io.sdram.cas_n := true.B
      io.sdram.we_n := true.B
    }
    is(SDRamState.reset_precharge_all) {
      io.sdram.dqm := Fill(dq/8, 1.U(1.W))
      io.sdram.cs_n := false.B
      io.sdram.ras_n := false.B
      io.sdram.cas_n := true.B
      io.sdram.we_n := false.B
      io.sdram.addr := "b10000000000".U
    }
    is(SDRamState.reset_set_mode) {
      io.sdram.dqm := Fill(dq/8, 1.U(1.W))
      when(delayCounter === (tMRD - 1).U) {
        io.sdram.cs_n := false.B
        io.sdram.ras_n := false.B
        io.sdram.cas_n := false.B
        io.sdram.we_n := false.B
        io.sdram.bank := 0.U
        io.sdram.addr := Cat(
          1.U(1.W), // Write Burst Mode
          0.U(2.W), // Operating Mode
          cas.U(3.W), // CAS
          0.U(1.W), // Burst type (Sequential / Interleaved)
          burstLengthModeCfgValue, // Burst Length
        ).asUInt()
      }
    }
    is(SDRamState.reset_auto_refresh, SDRamState.refresh) {
      when(delayCounter === (tRC - 1).U) {
        io.sdram.cs_n := false.B
        io.sdram.ras_n := false.B
        io.sdram.cas_n := false.B
        io.sdram.we_n := true.B
      }
    }

    is(SDRamState.idle) {
      io.sdram.cs_n := false.B
      io.sdram.ras_n := true.B
      io.sdram.cas_n := true.B
      io.sdram.we_n := true.B
    }

    is(SDRamState.precharge) {
      when(delayCounter === (tRP - 1).U) {
        io.sdram.cs_n := false.B
        io.sdram.ras_n := false.B
        io.sdram.cas_n := true.B
        io.sdram.we_n := false.B
        io.sdram.addr := Cat(1.U(1.W), 0.U(10.W)) // A10 = 1 select bank
        io.sdram.bank := bAddr
      }
    }
    is(SDRamState.row_activate) {
      when(delayCounter === (tRCD - 1).U) {
        io.sdram.cs_n := false.B
        io.sdram.ras_n := false.B
        io.sdram.cas_n := true.B
        io.sdram.we_n := true.B

        io.sdram.bank := bAddr
        io.sdram.addr := rowAddr
      }
    }
    is(SDRamState.issueRead) {
      io.sdram.cs_n := false.B
      io.sdram.ras_n := true.B
      io.sdram.cas_n := false.B
      io.sdram.we_n := true.B
      io.sdram.bank := bAddr
      if (ca < 10) {
        io.sdram.addr := Cat(0.U(1.W), 0.U((10-ca).W), colAddr)
      } else if (ca == 10) {
        io.sdram.addr := Cat(0.U(1.W), colAddr(9,0)) // noPrecharge
      } else {
        require(false, "Not supported yet")
      }
      io.sdram.dqm := 0.U
    }
    is(SDRamState.readWait) {
      io.sdram.dqm := 0.U
    }
    is(SDRamState.readLoad) {
      for(x <- 0 until burstSize) { // TODO: burst size
        when(delayCounter === x.U) {
          dataBuffer(x) := io.dataIn
        }
      }
    }
    is(SDRamState.issueWrite) {
      io.sdram.cs_n := false.B
      io.sdram.ras_n := true.B
      io.sdram.cas_n := false.B
      io.sdram.we_n := false.B
      io.sdram.output_en := true.B
      if (ca < 10) {
        io.sdram.addr := Cat(0.U(1.W), 0.U((10-ca).W), colAddr)
      } else {
        io.sdram.addr := Cat(0.U(1.W), colAddr(9,0)) // noPrecharge
      }
      io.sdram.bank := bAddr
      io.sdram.dqm := ~wbReqest.bits.sel(3,2)
      io.sdram.dataOut := wbReqest.bits.data(31, 16) // TODO width
    }
    is(SDRamState.writeData) {
      io.sdram.cs_n := false.B
      io.sdram.ras_n := true.B
      io.sdram.cas_n := false.B
      io.sdram.we_n := false.B
      io.sdram.output_en := true.B
      if (ca < 10) {
        print(ca)
        io.sdram.addr := Cat(0.U(1.W), 0.U((10-ca).W), colAddr(ca-1, 1), 1.U(1.W))
      } else {
        io.sdram.addr := Cat(0.U(1.W), colAddr(9,1), 1.U(1.W)) // noPrecharge
      }
      io.sdram.bank := bAddr
      io.sdram.dataOut := wbReqest.bits.data(15, 0)
      io.sdram.dqm := ~wbReqest.bits.sel(1, 0)
//      for(x <- 0 until (burstSize - 1)) { // TODO replace 1 with busW/dq - 1
//        when(delayCounter === x.U) {
//          io.sdram.dataOut := wbReqest.bits.data(x * dq + dq - 1, x * dq)
//          io.sdram.dqm := ~wbReqest.bits.sel((dq / 8) * (x+1) - 1, (dq / 8) * x)
//        }
//      }
    }
  }

  dontTouch(io.sdram)
}

