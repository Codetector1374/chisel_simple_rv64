package wb_device

import chisel3._
import chisel3.util.HasBlackBoxResource

class Memdev(lgMemSize: Int = 20) extends BlackBox(Map(
  "LGMEMSZ" -> lgMemSize
)) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val i_clk = Input(Clock())
    val i_wb_cyc = Input(Bool())
    val i_wb_stb = Input(Bool())
    val i_wb_we = Input(Bool())
    val i_wb_addr = Input(UInt((lgMemSize-2).W))
    val i_wb_data = Input(UInt(32.W))
    val i_wb_sel = Input(UInt(4.W))
    val o_wb_ack = Output(Bool())
    val o_wb_stall = Output(Bool())
    val o_wb_data = Output(UInt(32.W))
  })
  addResource("/memdev.v")
}

