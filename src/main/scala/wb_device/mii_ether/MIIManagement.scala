package wb_device.mii_ether

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

class SerializedMIIMgmtRequest extends Bundle {
  val preamble = UInt(32.W)
  val st = UInt(2.W)
  val op = UInt(2.W)
  val phyAddr = UInt(5.W)
  val regAddr = UInt(5.W)
}

class MIIManagement(sysClk: Int = 50000000, mdc: Int = 1000000) extends Module {
  val clkDiv = math.max(1, (sysClk / mdc) / 2)
  println(s"MII Management interface effective mdc clock: ${sysClk / clkDiv / 2.0}")

  val preamble = 32

  val io = IO(new Bundle {
    val mgmt = new MIIManagementIf
    val request = Flipped(new DecoupledIO(new MIIManagementRequest))
    val response = new Valid(new MIIManagementResponse)
  })

  val clkDivCounter = RegInit((clkDiv - 1).U(24.W))
  val mgmtClk = RegInit(false.B)
  io.mgmt.clk := mgmtClk
  clkDivCounter := clkDivCounter - 1.U
  when(clkDivCounter === 0.U) {
    clkDivCounter := (clkDiv - 1).U
    mgmtClk := !mgmtClk
  }

  object State extends ChiselEnum {
    val idle, request, ta, writeData, readData = Value
  }

  val mgmtState = RegInit(State.idle)

  val currentRequest = Reg(chiselTypeOf(io.request.bits))
  val encodedRequest = Wire(new SerializedMIIMgmtRequest)
  encodedRequest.preamble := "hFFFFFFFF".U(32.W)
  encodedRequest.st := "b01".U(2.W)
  when(currentRequest.bWrite) {
    encodedRequest.op := "b01".U(2.W)
  }.otherwise {
    encodedRequest.op := "b10".U(2.W)
  }
  encodedRequest.phyAddr := currentRequest.phyAddr
  encodedRequest.regAddr := currentRequest.regAddr

  val counter = RegInit(0.U(8.W))
  val readBuffer = Reg(Vec(16, Bool()))
  val mdo = Wire(Bool())
  mdo := false.B

  io.request.ready := false.B
  io.response.valid := mgmtState === State.idle
  io.response.bits.value := readBuffer.asUInt()
  io.mgmt.dataOut := false.B

  io.mgmt.outEn := true.B
  switch(mgmtState) {
    is(State.idle) {
      io.mgmt.outEn := false.B
    }
    is(State.request) {
      io.mgmt.dataOut := encodedRequest.asUInt()(counter).asBool()
    }
    is(State.ta) {
      when(!currentRequest.bWrite) {
        io.mgmt.outEn := false.B
      }.otherwise {
        io.mgmt.dataOut := counter(0);
      }
    }
    is(State.writeData) {
      io.mgmt.dataOut := currentRequest.wrData(counter)
    }
    is(State.readData) {
      io.mgmt.outEn := false.B
    }
  }

  when(clkDivCounter === 0.U && mgmtClk === true.B) {
    counter := counter - 1.U
    switch(mgmtState) {
      is(State.idle) {
        io.request.ready := true.B
        when(io.request.valid) {
          currentRequest := io.request.bits
          mgmtState := State.request
          counter := (encodedRequest.getWidth - 1).U
        }
      }
      is(State.request) {
        when(counter === 0.U) {
          mgmtState := State.ta
          counter := 1.U
        }
      }
      is(State.ta) {
        when(counter === 0.U) {
          counter := 15.U
          when(currentRequest.bWrite) {
            mgmtState := State.writeData
          }.otherwise {
            readBuffer(15) := io.mgmt.dataIn
            mgmtState := State.readData
          }
        }
      }
      is(State.writeData) {
        when(counter === 0.U) {
          mgmtState := State.idle
        }
      }
      is(State.readData) {
        when(counter === 0.U) {
          mgmtState := State.idle
        }.otherwise {
          readBuffer(counter - 1.U) := io.mgmt.dataIn
        }
      }
    }
  }
}

