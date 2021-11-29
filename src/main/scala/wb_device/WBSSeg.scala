package wb_device

import chisel3._
import wb_device.sseg.SSEG
import wishbone.Wishbone

class WBSSeg(numSSeg: Int = 8) extends Module {
  val numLeds = numSSeg * 4;
  require(numLeds <= 32)
  val numBytes = Math.ceil(numLeds / 8.0).toInt

  val io = IO(new Bundle {
    val ssegs = Output(Vec(numSSeg, UInt(7.W)))
    val wb = new Wishbone
  })
  io.wb.stall := false.B
  io.wb.error := false.B

  val dataBuf = RegInit(VecInit(Seq.fill(numBytes)(0.U(8.W))))

  val sseg = Module(new SSEG(numSSeg))
  sseg.io.value := dataBuf.asUInt()
  io.ssegs := sseg.io.ssegs

  io.wb.miso_data := dataBuf.asUInt()
  io.wb.ack := false.B
  when(io.wb.cyc && io.wb.stb) {
    io.wb.ack := true.B
    when(io.wb.we) {
      for(x <- 0 until numBytes) {
        when(io.wb.sel(x)) {
          dataBuf(x) := io.wb.mosi_data(8 * (x + 1) - 1, 8 * x)
        }.otherwise {
          dataBuf(x) := 0.U
        }
      }
    }
  }

}

