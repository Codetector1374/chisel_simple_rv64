package simple_rv32

import chisel3._
import chisel3.util._

class IDRRBuf extends Bundle {
  val opcode = Output(UInt(7.W))
  val pc = Output(UInt(32.W))
  val func7 = Output(UInt(7.W))
  val func3 = Output(UInt(3.W))
  val rs1Val = Output(UInt(32.W))
  val rs2Val = Output(UInt(32.W))
  val rd = Output(UInt(5.W))
  val imm = Output(UInt(32.W))
}

class DecodedInstruction extends Bundle {
  val opcode = Output(UInt(7.W))
  val func7 = Output(UInt(7.W))
  val func3 = Output(UInt(3.W))
  val rs1 = Output(UInt(5.W))
  val rs2 = Output(UInt(5.W))
  val rd = Output(UInt(5.W))
  val imm = Output(UInt(32.W))
}

class IDRR(aftStageCount: Int = 2) extends Module {
  val io = IO(new Bundle {
    val pipeSignalsOut = Flipped(new PipelineStageSignals)
    val pipeSignalsIn = new PipelineStageSignals

    val operandForward = Vec(aftStageCount, new OperandForward)

    val fetch = Flipped(new FetchBuffer)

    val port1 = Flipped(new RegFileReadPort)
    val port2 = Flipped(new RegFileReadPort)

    val out = new IDRRBuf
  })
  // TODO: remove
  io <> DontCare
  val outBuf = RegInit(0.U.asTypeOf(new IDRRBuf))
  io.out := outBuf

  val generateStall = Wire(Bool())
  generateStall := false.B
  io.pipeSignalsOut.pipeStall := generateStall || io.pipeSignalsIn.pipeStall
  io.pipeSignalsOut.pipeFlush := io.pipeSignalsIn.pipeFlush

  val inst = io.fetch.inst
  val decodedInst = Wire(new DecodedInstruction)
  decodedInst := 0.U.asTypeOf(new DecodedInstruction)
  val opcode = inst(6, 0)
  decodedInst.opcode := opcode
  when(opcode =/= RV32Opcode.NOP) {
    // RS1
    when(!(opcode === RV32Opcode.LUI || opcode === RV32Opcode.AUIPC || opcode === RV32Opcode.JAL)) {
      decodedInst.rs1 := inst(19, 15)
    }

    // RS2
    when(opcode === RV32Opcode.BR || opcode === RV32Opcode.STORE || opcode === RV32Opcode.ALU) {
      decodedInst.rs2 := inst(24, 20)
    }

    // RD
    when(opcode === RV32Opcode.LUI || opcode === RV32Opcode.AUIPC
      || opcode === RV32Opcode.JAL || opcode === RV32Opcode.JALR
      || opcode === RV32Opcode.LOAD || opcode === RV32Opcode.ALUI
      || opcode === RV32Opcode.ALU)
    {
      decodedInst.rd := inst(11, 7)
    }

    // imm
    switch(opcode) {
      is(RV32Opcode.LUI, RV32Opcode.AUIPC) {
        decodedInst.imm := Cat(inst(31, 12), 0.U(12.W))
      }
      is(RV32Opcode.JAL) {
        decodedInst.imm := Cat(Fill(12, inst(31)), inst(19, 12), inst(20), inst(30, 25), inst(24,21), 0.U(1.W))
      }
      is(RV32Opcode.ALUI, RV32Opcode.LOAD, RV32Opcode.JALR) {
        decodedInst.imm := Cat(Fill(20, inst(31)), inst(31, 20))
      }
      is(RV32Opcode.STORE) {
        decodedInst.imm := Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))
      }
      is(RV32Opcode.BR) {
        // ----------------------------20 --------- 1 --------- 6 ------------ 4 ----------1
        decodedInst.imm := Cat(Fill(20, inst(31)), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
      }
    }

    decodedInst.func7 := inst(31, 25)
    decodedInst.func3 := inst(14, 12)

  }

  io.port1.regNo := decodedInst.rs1
  io.port2.regNo := decodedInst.rs2

  when(!io.pipeSignalsIn.pipeStall) { // Not an external stall
    outBuf := 0.U.asTypeOf(new IDRRBuf)
  }
  when(!io.pipeSignalsIn.pipeFlush) {
    // Check internal stall condition
    for(x <- 0 until aftStageCount) {
      when((io.operandForward(x).regNo =/= 0.U)
        && (io.operandForward(x).regNo === decodedInst.rs1 || io.operandForward(x).regNo === decodedInst.rs2)) {
        generateStall := true.B
      }
    }

    when(!io.pipeSignalsOut.pipeStall) {
      outBuf.opcode := opcode
      outBuf.pc := io.fetch.pc
      outBuf.func7 := decodedInst.func7
      outBuf.func3 := decodedInst.func3
      outBuf.rs1Val := io.port1.data
      outBuf.rs2Val := io.port2.data
      outBuf.rd := decodedInst.rd
      outBuf.imm := decodedInst.imm
    }
  }
}

