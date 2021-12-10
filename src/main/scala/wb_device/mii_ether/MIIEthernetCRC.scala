package wb_device.mii_ether

import chisel3._
import chisel3.util.{Reverse, Valid}

class MIIEthernetCRC extends Module {
  val io = IO(new Bundle {
    val clear = Input(Bool())
    val data = Input(Valid(UInt(4.W)))
    val crc_out = Output(UInt(32.W))
  })

  val crcmod = Module(new CRC32)
  crcmod.io.clk := clock
  crcmod.io.rst := (reset.asBool() || io.clear)
  crcmod.io.crc_en := io.data.valid
  crcmod.io.data_in := Reverse(io.data.bits)
  io.crc_out := ~(Reverse(crcmod.io.crc_out))
}

