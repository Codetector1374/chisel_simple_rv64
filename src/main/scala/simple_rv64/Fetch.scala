package simple_rv64

import chisel3._
import wishbone.Wishbone


class FetchBuffer extends Bundle {
  val pc = Output(UInt(64.W))
  val inst = Output(UInt(32.W))
}

class Fetch extends Module with RequireSyncReset {
  val io = IO(new Bundle {
    val pipeFlush = Input(Bool())
    val pipeStall = Input(Bool())
    val wb = Flipped(new Wishbone)
    val buf = new FetchBuffer
  })

  val outBuffer = RegInit(0.U.asTypeOf(new FetchBuffer))

  val pc = RegInit(0.U(64.W))

  val imem = Reg(Vec(32, UInt(32.W)))

  when(io.pipeFlush) {
    outBuffer := 0.U.asTypeOf(new FetchBuffer)
  } .elsewhen (!io.pipeStall) {
    pc := pc + 4.U(64.W)
    outBuffer.pc := pc
    outBuffer.inst := imem((pc >> 2.U).asUInt())
  }

  io.wb <> DontCare
  io.buf := outBuffer
}
