package main

import chisel3._
import freechips.asyncqueue.AsyncQueue

class CDCExperimentTop extends RawModule {
  val io = IO(new Bundle {
    val gReset = Input(Bool())
    val srcClk = Input(Clock())
    val srcValue = Input(UInt(8.W))
    val srcValid = Input(Bool())

    val dstClk = Input(Clock())
    val dstValue = Output(UInt(8.W))
    val dstValid = Output(Bool())
  })

  val asyncQueue = withClockAndReset(io.srcClk, io.gReset){Module(new AsyncQueue(UInt(8.W)))}
  asyncQueue.io.enq_clock := io.srcClk
  asyncQueue.io.enq_reset := io.gReset

  asyncQueue.io.enq.valid := io.srcValid && asyncQueue.io.enq.ready
  asyncQueue.io.enq.bits := io.srcValue

  asyncQueue.io.deq_clock := io.dstClk
  asyncQueue.io.deq_reset := io.gReset

  io.dstValid := asyncQueue.io.deq.valid
  io.dstValue := asyncQueue.io.deq.bits
  asyncQueue.io.deq.ready := true.B

}

