package wb_device.mii_ether

import chisel3._
import chisel3.util.HasBlackBoxResource

class CRC32 extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    val data_in = Input(UInt(4.W))
    val crc_en = Input(Bool())
    val crc_out = Output(UInt(32.W))
  })
  addResource("/crc32.v")
}

