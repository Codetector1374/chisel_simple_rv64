package simple_rv32

import chisel3._
import chisel3.experimental.ChiselEnum
import wishbone.Wishbone


class FetchBuffer(pcWidth: Int = 32, instWidth: Int = 32) extends Bundle {
  val pc = Output(UInt(pcWidth.W))
  val inst = Output(UInt(instWidth.W))
}


class Fetch(pcWidth: Int = 32, instWidth: Int = 32) extends Module {
  object WBState extends ChiselEnum {
    val idle, strobe, waitAck, err = Value
  }

  val io = IO(new Bundle {
    val pipeSignalsIn = new PipelineStageSignals

    val loadPC = Input(Bool())
    val newPC = Input(UInt(pcWidth.W))

    val wb = Flipped(new Wishbone)
    val buf = new FetchBuffer

    val currPC = Output(UInt(32.W))
  })
  val fetchBuf = RegInit(0.U.asTypeOf(new FetchBuffer))
  val pc = RegInit(0.U(pcWidth.W))
  val wbState = RegInit(WBState.idle)

  io.currPC := pc

  io.wb <> DontCare
  io.wb.stb := (wbState === WBState.strobe)
  io.wb.cyc := (wbState === WBState.waitAck || io.wb.stb)
  io.wb.addr := (pc >> 2.U).asUInt()
  io.wb.sel := "b1111".U

  def completeFetch(): Unit = {
    wbState := WBState.idle
    when(!io.pipeSignalsIn.pipeStall) {
      fetchBuf.pc := pc
      fetchBuf.inst := io.wb.miso_data
      pc := pc + (instWidth / 8).U
    }
  }

  when (io.loadPC) {
    pc := io.newPC // Loads new PC on loadPC
  }

  when(wbState =/= WBState.err) {
    when(io.pipeSignalsIn.pipeFlush) { // Clean the output buffers when flush happens
      fetchBuf := 0.U.asTypeOf(new FetchBuffer)
      when (wbState =/= WBState.idle) {
        wbState := WBState.idle // put to idle to abort the wb txn
      }
    }.elsewhen(!io.pipeSignalsIn.pipeStall) {
      fetchBuf := 0.U.asTypeOf(new FetchBuffer)
    }
  }

  io.wb.we := false.B
  when(wbState === WBState.idle && !io.pipeSignalsIn.pipeStall) { // if we are at idle, we should start a fetch cycle
    wbState := WBState.strobe
  }
  .elsewhen(wbState === WBState.strobe) {
    when(!io.wb.stall) {
      when(io.wb.ack) {
        completeFetch()
      }.elsewhen(io.wb.error) {
        wbState := WBState.err
      }.otherwise {
        wbState := WBState.waitAck
      }
    }
  } .elsewhen(wbState === WBState.waitAck) {
    when(io.wb.ack) {
      completeFetch()
    }.elsewhen(io.wb.error) {
      wbState := WBState.err
    }
  }

  io.buf := fetchBuf

}
