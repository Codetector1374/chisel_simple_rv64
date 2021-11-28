package yunjue

import chisel3._
import wishbone.Wishbone

class YJTopWrapper extends Module {
  val io = IO(new Bundle {
    val procReset = Input(Bool())
    val fetchWb = Flipped(new Wishbone)
    val memWb = Flipped(new Wishbone)
  })

  val top = Module(new yunjue.top())

  top.io.clk := clock
  top.io.reset := (reset.asBool() || io.procReset)

  val fetchWb = io.fetchWb
  fetchWb.cyc <> top.io.wb2_cyc
  fetchWb.stb <> top.io.wb2_stb
  fetchWb.we <> top.io.wb2_we
  fetchWb.sel <> top.io.wb2_sel
  fetchWb.stall <> top.io.wb2_stall
  fetchWb.ack <> top.io.wb2_ack
  fetchWb.addr <> top.io.wb2_addr
  fetchWb.mosi_data <> top.io.wb2_mosi_data
  fetchWb.miso_data <> top.io.wb2_miso_data

  val memWb = io.memWb
  memWb.cyc <> top.io.wb_cyc
  memWb.stb <> top.io.wb_stb
  memWb.we <> top.io.wb_we
  memWb.sel <> top.io.wb_sel
  memWb.stall <> top.io.wb_stall
  memWb.ack <> top.io.wb_ack
  memWb.addr <> top.io.wb_addr
  memWb.mosi_data <> top.io.wb_mosi_data
  memWb.miso_data <> top.io.wb_miso_data
}

