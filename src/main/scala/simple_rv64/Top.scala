package simple_rv64

import chisel3._
import wb_device.Memdev

class Top extends Module {
  val io = IO(new Bundle {
  })

  val memdev = Module(new Memdev())
  memdev.io <> DontCare

  val fetch = Module(new Fetch)
  fetch.io <> DontCare

  val fetchWb = fetch.io.wb
  memdev.io.i_clk := clock

  dontTouch(fetch.io)
}

