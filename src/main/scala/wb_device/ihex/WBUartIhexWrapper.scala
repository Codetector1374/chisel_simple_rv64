package wb_device.ihex

import chisel3._
import wishbone.Wishbone

class WBUartIhexWrapper(clockFreq: Long = 50000000, baud: Long = 115200) extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())

    val masterWb = Flipped(new Wishbone)
    val slaveWb = new Wishbone
  })

  val m = Module(new WBUartWithIhex(clockFreq, baud))
  m.io.i_clk := clock
  m.io.i_reset := reset

  m.io.i_rx := io.rx
  io.tx := m.io.o_tx

  val mwb = io.masterWb
  mwb.cyc <> m.io.o_mwb_cyc
  mwb.stb <> m.io.o_mwb_stb
  mwb.we <> m.io.o_mwb_we
  mwb.sel <> m.io.o_mwb_sel
  mwb.stall <> m.io.i_mwb_stall
  mwb.ack <> m.io.i_mwb_ack
  mwb.error <> m.io.i_mwb_err
  mwb.addr <> m.io.o_mwb_addr
  mwb.mosi_data <> m.io.o_mwb_data
  mwb.miso_data <> m.io.i_mwb_data

  val swb = io.slaveWb
  swb.cyc <> m.io.i_wb_cyc
  swb.stb <> m.io.i_wb_stb
  swb.we <> m.io.i_wb_we
  swb.sel <> m.io.i_wb_sel
  swb.stall <> m.io.o_wb_stall
  swb.ack <> m.io.o_wb_ack
  swb.error <> m.io.o_wb_err
  swb.addr <> m.io.i_wb_addr
  swb.mosi_data <> m.io.i_wb_data
  swb.miso_data <> m.io.o_wb_data
}

