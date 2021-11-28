package wb_device

import chisel3._
import chisel3.util.HasBlackBoxResource

class SinglePortRam(aw: Int = 14) extends BlackBox(Map(
  "AW" -> aw
)) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val addr = Input(UInt(aw.W))
    val we = Input(Bool())
    val data_out = Output(UInt(8.W))
    val data_in = Input(UInt(8.W))
  })

  addResource("/SinglePortRam.v")

}

