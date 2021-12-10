package wb_device.mii_ether

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import firrtl.PrimOps
import firrtl.PrimOps.Pad
import freechips.asyncqueue.{AsyncQueue, AsyncQueueParams}
import wb_device.SinglePortRam

class MIIEthernetTX extends RawModule {
  val io = IO(new Bundle {
    val sysClk = Input(Clock())
    val reset = Input(Bool())

    val reqEnq = Flipped(Decoupled(new MIITxRequest))
    val txBusy = Output(Bool())
    val miiTx = new MIITx
  })


  val requestQueue = withClockAndReset(io.sysClk, io.reset)(Module(new AsyncQueue(new MIITxRequest, AsyncQueueParams(depth = 8, safe = false))))
  requestQueue.io.enq_clock := io.sysClk
  requestQueue.io.enq_reset := io.reset
  requestQueue.io.deq_clock := io.miiTx.clk
  requestQueue.io.deq_reset := io.reset
  requestQueue.io.enq <> io.reqEnq

  val deq = requestQueue.io.deq
  withClockAndReset(io.miiTx.clk, io.reset) {
    io.miiTx.err := false.B
    io.miiTx.en := false.B
    io.miiTx.data := DontCare

    val crc = Module(new MIIEthernetCRC)
    crc.io.clear := false.B
    crc.io.data.valid := false.B
    crc.io.data.bits := DontCare

    object SendState extends ChiselEnum {
      val idle, preamble, sof, payload, padding, checksum = Value
    }
    val sendState = RegInit(SendState.idle)

    withClockAndReset(io.sysClk, io.reset) {
      val cdcRegs = Array.fill(3)(Reg(Bool()))
      for (x <- 0 until cdcRegs.length - 1) {
        cdcRegs(x) := cdcRegs(x + 1)
      }
      io.txBusy := cdcRegs(0)
      cdcRegs(cdcRegs.length - 1) := sendState =/= SendState.idle
    }

    val sendCntr = RegInit(0.U(11.W))
    val byteCntr = RegInit(0.U(11.W))
    val timeCntr = RegInit(0.U(16.W))
    val ram = Module(new SinglePortRam(aw = 11))
    ram.io.clock := io.miiTx.clk
    ram.io.we := false.B
    ram.io.data_in := DontCare
    when(sendState === SendState.idle) { // waiting for push bytes
      ram.io.addr := byteCntr
    }.elsewhen(sendState === SendState.sof) {
      ram.io.addr := sendCntr
    }.otherwise {
      ram.io.addr := sendCntr + 1.U
    }

    val ramContentBuffer = Reg(UInt(8.W))

    timeCntr := timeCntr - 1.U

    deq.ready := false.B
    when(sendState === SendState.idle) {
      deq.ready := true.B
      when(deq.valid) {
        switch(deq.bits.reqType) {
          is(MIITxRequestType.pushByte) {
            ram.io.data_in := deq.bits.payload
            ram.io.we := true.B
            byteCntr := byteCntr + 1.U
          }
          is(MIITxRequestType.send) {
            when(byteCntr =/= 0.U) {
              sendState := SendState.preamble
              timeCntr := (14 - 1).U
            }
          }
        }
      }
    }

    switch(sendState) {
      is(SendState.preamble) {
        when(timeCntr === 0.U) {
          timeCntr := 1.U
          sendState := SendState.sof
        }

        io.miiTx.en := true.B
        io.miiTx.data := "h5".U(4.W)
      }

      is(SendState.sof) {
        when(timeCntr === 0.U) {
          timeCntr := 1.U
          ramContentBuffer := ram.io.data_out
          sendState := SendState.payload
          crc.io.clear := true.B
        }

        when(timeCntr === 1.U) {
          io.miiTx.data := "h5".U(4.W)
        }.otherwise {
          io.miiTx.data := "hD".U(4.W)
        }
        io.miiTx.en := true.B
      }


      is(SendState.payload) {
        when(timeCntr === 0.U) {
          timeCntr := 1.U
          ramContentBuffer := ram.io.data_out
          sendCntr := sendCntr + 1.U
          when(sendCntr === (byteCntr - 1.U)) {
            when(sendCntr < (64 - 4 - 1).U) {
              sendState := SendState.padding
            }.otherwise {
              sendState := SendState.checksum
            }
          }
        }

        when(timeCntr === 1.U) {
          crc.io.data.bits := ramContentBuffer(3, 0)
          io.miiTx.data := ramContentBuffer(3, 0)
        }.otherwise {
          crc.io.data.bits := ramContentBuffer(7, 4)
          io.miiTx.data := ramContentBuffer(7, 4)
        }
        crc.io.data.valid := true.B
        io.miiTx.en := true.B
      }

      is(SendState.padding) {
        when(timeCntr === 0.U) {
          timeCntr := 1.U
          sendCntr := sendCntr + 1.U
          when(sendCntr === (64 - 4 - 1).U) {
            sendState := SendState.checksum
            timeCntr := 7.U
          }
        }

        crc.io.data.bits := 0.U
        io.miiTx.data := 0.U
        crc.io.data.valid := true.B
        io.miiTx.en := true.B
      }

      is(SendState.checksum) {
        when(timeCntr === 0.U) {
          sendState := SendState.idle
          byteCntr := 0.U
          sendCntr := 0.U
        }
        for (i <- 0 until 8) {
          when(timeCntr === i.U) {
            io.miiTx.data := crc.io.crc_out(4 * (8 - i) - 1, 4 * (7-i))
          }
        }
        io.miiTx.en := true.B
      }
    }
  }
}

