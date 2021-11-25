package simple_rv32

import chisel3._
import wb_device.Memdev
import wishbone.WBArbiter

class Top extends Module {
  val io = IO(new Bundle {
  })

  val fetch = Module(new Fetch)
  fetch.io <> DontCare

  val dprf = Module(new DPRF())
  dprf.io <> DontCare
  dontTouch(dprf.io)

  val idrr = Module(new IDRR())
  idrr.io <> DontCare
  fetch.io.pipeSignalsIn <> idrr.io.pipeSignalsOut
  idrr.io.port1 <> dprf.io.read1
  idrr.io.port2 <> dprf.io.read2
  idrr.io.fetch <> fetch.io.buf
  idrr.io.operandForward(0).regNo := idrr.io.out.rd
  dontTouch(idrr.io)

  val ex = Module(new Execute)
  ex.io <> DontCare
  ex.io.idrr <> idrr.io.out
  ex.io.pipeSignalsOut <> idrr.io.pipeSignalsIn
  idrr.io.operandForward(1).regNo := ex.io.out.regNo
  dprf.io.write <> ex.io.out
  dontTouch(ex.io)


  // Wishbone Stuff
  val arb = Module(new WBArbiter(numConn = 2))
  arb.io.masters(0) <> fetch.io.wb
  arb.io.masters(1) <> ex.io.wb

  val memdev = Module(new Memdev())
  val theWb = arb.io.output
  memdev.io.i_clk := clock
  memdev.io.i_wb_cyc := theWb.cyc
  memdev.io.i_wb_stb := theWb.stb
  memdev.io.i_wb_we := theWb.we
  memdev.io.i_wb_sel := theWb.sel
  memdev.io.i_wb_addr := theWb.addr
  memdev.io.i_wb_data := theWb.mosi_data
  theWb.miso_data := memdev.io.o_wb_data
  theWb.ack := memdev.io.o_wb_ack
  theWb.stall :=  memdev.io.o_wb_stall
  theWb.error := false.B

  dontTouch(fetch.io)
}

