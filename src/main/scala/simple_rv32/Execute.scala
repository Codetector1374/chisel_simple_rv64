package simple_rv32

import chisel3._
import chisel3.util._
import simple_rv32.ex.{ALU, BranchUnit, LoadStore}
import wishbone.Wishbone

class ExOut extends Bundle {
  val rd = Output(UInt(5.W))
  val rdVal = Output(UInt(32.W))
}

class Execute extends Module {
  val io = IO(new Bundle {
    val pipeSignalsOut = Flipped(new PipelineStageSignals)

    val idrr = Flipped(new IDRRBuf)
    val wb = Flipped(new Wishbone())
    val dprf_write = Flipped(new RegFileWritePort)

    val doBranch = Output(Bool())
    val targetPc = Output(UInt(32.W))
  })
  val outBuf = RegInit(0.U.asTypeOf(new ExOut))
  io.dprf_write.we := true.B
  io.dprf_write.regNo := outBuf.rd
  io.dprf_write.data := outBuf.rdVal
  io.doBranch := false.B
  io.targetPc := DontCare

  val idrr = io.idrr
  val opcode = idrr.opcode

  val alu1 = Module(new ALU)
  val aluResult = Wire(UInt(32.W))
  aluResult := alu1.io.result;
  val alu1io = alu1.io
  alu1io.opcode := idrr.opcode
  alu1io.pc := idrr.pc
  alu1io.func7 := idrr.func7
  alu1io.func3 := idrr.func3
  alu1io.rs1Val := idrr.rs1Val
  alu1io.rs2Val := idrr.rs2Val
  alu1io.imm := idrr.imm

  val lsu1 = Module(new LoadStore)
  lsu1.io.opcode := idrr.opcode
  lsu1.io.func3 := idrr.func3
  lsu1.io.rs1Val := idrr.rs1Val
  lsu1.io.rs2Val := idrr.rs2Val
  lsu1.io.imm := idrr.imm
  lsu1.io.wb <> io.wb
  dontTouch(lsu1.io.wb)

  val bru = Module(new BranchUnit)
  bru.io.opcode := idrr.opcode
  bru.io.pc := idrr.pc
  bru.io.func3 := idrr.func3
  bru.io.rs1Val := idrr.rs1Val
  bru.io.rs2Val := idrr.rs2Val
  bru.io.imm := idrr.imm


  val result = Wire(UInt(32.W))
  result := DontCare
  io.pipeSignalsOut := 0.U.asTypeOf(Flipped(new PipelineStageSignals))
  switch(opcode) {
    is(RV32Opcode.ALU, RV32Opcode.ALUI, RV32Opcode.LUI, RV32Opcode.AUIPC) {
      result := aluResult
    }
    is(RV32Opcode.LOAD, RV32Opcode.STORE) {
      io.pipeSignalsOut.pipeStall := lsu1.io.busy
      result := lsu1.io.result
    }
    is(RV32Opcode.BR, RV32Opcode.JAL, RV32Opcode.JALR) {
      io.pipeSignalsOut.pipeFlush := bru.io.doBranch
      io.doBranch := bru.io.doBranch
      io.targetPc := bru.io.branchTarget
      result := bru.io.result
    }
  }

  outBuf.rdVal := result
  when (io.pipeSignalsOut.pipeStall) {
    outBuf.rd := 0.U
  } .otherwise {
    outBuf.rd := idrr.rd
  }
}

