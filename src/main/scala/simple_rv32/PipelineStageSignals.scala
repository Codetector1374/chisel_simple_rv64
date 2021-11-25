package simple_rv32

import chisel3._

class PipelineStageSignals extends Bundle {
  val pipeFlush = Input(Bool())
  val pipeStall = Input(Bool())
}

