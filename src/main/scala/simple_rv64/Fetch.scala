package simple_rv64

import chisel3._
import wishbone.Wishbone

class FetchBuffer extends Bundle {

}

class Fetch extends Module {
  val io = IO(new Bundle {
    val pipeFlush = Input(Bool())
    val pipeStall = Input(Bool())
    val wb = Flipped(new Wishbone)
  })

  io.wb <> DontCare

}
