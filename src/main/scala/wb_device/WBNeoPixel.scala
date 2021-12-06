package wb_device

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import wishbone.Wishbone

class LEDColor extends Bundle {
  val green = UInt(8.W)
  val red = UInt(8.W)
  val blue = UInt(8.W)
}

class WBNeoPixel(clockSpeed: Long = 50000000, maxNumLeds: Int = 256) extends Module {

  val t0h = 0.00000045D
  val t1l = 0.0000005D
  val t1h = 0.00000085D
  val t0l = 0.0000009D
  val tRes = 0.0001D

  val t0hCyc = (t0h * clockSpeed).round.toInt
  val t1lCyc = (t1l * clockSpeed).round.toInt
  val t1hCyc = (t1h * clockSpeed).round.toInt
  val t0lCyc = (t0l * clockSpeed).round.toInt
  val tResCyc = (tRes * clockSpeed).toInt

  println(s"t0h = $t0hCyc, t1l = $t1lCyc, t1h = $t1hCyc, t0l = $t0lCyc, tResCyc = $tResCyc")

  val io = IO(new Bundle {
    val wb = new Wishbone()
    val led = Output(Bool())
  })
  dontTouch(io)

  def makeColor(color: String): Data = {
    color.U.asTypeOf(new LEDColor)
  }

  val mem = SyncReadMem(maxNumLeds, new LEDColor)

  // Output mem data
  object OutputState extends ChiselEnum {
    val idle, output, output_reset = Value
  }

  val oState = RegInit(OutputState.idle)
  val oLEDCntr = Reg(UInt(8.W))
  val oLEDBitCntr = Reg(UInt(5.W))
  val oTimeCntr = Reg(UInt(16.W))

  oTimeCntr := oTimeCntr + 1.U

  switch(oState) {
    is(OutputState.idle) {
      // when (start send)
      oLEDBitCntr := 23.U
      oLEDCntr := 0.U
      oTimeCntr := 0.U
      oState := OutputState.output
    }
    is(OutputState.output) {
      when(oTimeCntr >= (t0hCyc + t0lCyc - 1).U) {
        oLEDBitCntr := oLEDBitCntr - 1.U
        oTimeCntr := 0.U

        when(oLEDBitCntr === 0.U) {
          oLEDBitCntr := 23.U // TODO magic number bad
          oLEDCntr := oLEDCntr + 1.U
          oTimeCntr := 0.U
          when(oLEDCntr === (maxNumLeds - 1).U) {
            oState := OutputState.output_reset
          }
        }
      }
    }
    is(OutputState.output_reset) {
      when(oTimeCntr >= tResCyc.U) {
        oState := OutputState.idle
      }
    }
  }

  val currentLED = mem(oLEDCntr).asUInt()

  io.led := false.B
  io.wb.ack := false.B
  io.wb.miso_data := DontCare
  io.wb.error := false.B
  // Combinational Output part
  switch(oState) {
    is(OutputState.output) {
      when(currentLED(oLEDBitCntr).asBool()) { // 1
        io.led := (oTimeCntr < t1hCyc.U)
      }.otherwise { // 0
        io.led := (oTimeCntr < t0hCyc.U)
      }
    }
  }

  io.wb.stall := false.B
  when(io.wb.cyc && io.wb.stb) {
    when(io.wb.we) {
      io.wb.ack := true.B
      mem(io.wb.addr(7, 0)) := io.wb.mosi_data(23, 0).asTypeOf(new LEDColor)
    }
  }
}