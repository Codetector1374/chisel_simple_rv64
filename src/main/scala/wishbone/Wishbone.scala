package wishbone

import chisel3._
/*
    o_wb_cyc, o_wb_stb, o_wb_we,
    o_wb_addr, o_wb_data, o_wb_sel,
    i_wb_ack, i_wb_stall, i_wb_err,
    i_wb_data,
 */
class Wishbone(val addrWidth: Int = 32, val dataWidth: Int = 32) extends Bundle {
  require(addrWidth > 0)
  require(dataWidth > 8)
  require((dataWidth % 8) == 0, "dataWidth needs to be a multiple of 8");
  private val numOfBytes = dataWidth / 8;

  val cyc = Input(Bool())
  val stb = Input(Bool())
  val we = Input(Bool())
  val addr = Input(UInt(addrWidth.W))
  val mosi_data = Input(UInt(dataWidth.W))
  val miso_data = Output(UInt(dataWidth.W))
  val sel = Input(UInt(numOfBytes.W))
  val ack = Output(Bool())
  val stall = Output(Bool())
  val error = Output(Bool())
}
