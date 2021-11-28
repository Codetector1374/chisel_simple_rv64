package wb_device

import chisel3._
import chisel3.util.Fill
import wishbone.Wishbone

class WBLeds(numLeds: Int = 16, activeLow: Boolean = false) extends Module {
  require(numLeds <= 32)
  val numBytes = Math.ceil(numLeds / 8.0).toInt

  val io = IO(new Bundle {
    val leds = Output(UInt(numLeds.W))

    val wb = new Wishbone
  })
  io.wb.stall := false.B
  io.wb.error := false.B

  val ledBuf = RegInit(VecInit(Seq.fill(numBytes)(0.U(8.W))))
  if (activeLow) {
    io.leds := ~(ledBuf.asUInt())
  } else {
    io.leds := ledBuf.asUInt()
  }

  io.wb.miso_data := ledBuf.asUInt()
  io.wb.ack := false.B
  when(io.wb.cyc && io.wb.stb) {
    io.wb.ack := true.B
    when(io.wb.we) {
      for(x <- 0 until numBytes) {
        when(io.wb.sel(x)) {
          ledBuf(x) := io.wb.mosi_data(8 * (x + 1) - 1, 8 * x)
        }
      }
    }
  }

}

