package simple_rv32.ex

import chisel3._
import chisel3.util._
import simple_rv32.{BrFunc3, RV32Opcode}

class BranchUnit extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val pc = Input(UInt(32.W))
    val func3 = Input(UInt(3.W))
    val rs1Val = Input(UInt(32.W))
    val rs2Val = Input(UInt(32.W))
    val imm = Input(UInt(32.W))

    val result = Output(UInt(32.W))
    val doBranch = Output(Bool())
    val branchTarget = Output(UInt(32.W))
  })

  val incrementedPc = Wire(UInt(32.W))
  incrementedPc := io.pc + 4.U

  io.result := DontCare
  io.doBranch := false.B
  io.branchTarget := DontCare
  switch(io.opcode) {
    is(RV32Opcode.JAL) {
      io.result := incrementedPc
      io.doBranch := true.B
      io.branchTarget := io.pc + io.imm
    }
    is(RV32Opcode.JALR) {
      io.branchTarget := io.rs1Val + io.imm
      io.doBranch := true.B
      io.result := incrementedPc
    }
    is(RV32Opcode.BR) {
      io.branchTarget := io.pc + io.imm
      switch(io.func3) {
        is(BrFunc3.Eq) {
          io.doBranch := io.rs1Val === io.rs2Val
        }
        is(BrFunc3.Ne) {
          io.doBranch := io.rs1Val =/= io.rs2Val
        }
        is(BrFunc3.Ge) {
          io.doBranch := io.rs1Val.asSInt() >= io.rs2Val.asSInt()
        }
        is(BrFunc3.Lt) {
          io.doBranch := io.rs1Val.asSInt() < io.rs2Val.asSInt()
        }
        is(BrFunc3.LtU) {
          io.doBranch := io.rs1Val < io.rs2Val
        }
        is(BrFunc3.GeU) {
          io.doBranch := io.rs1Val >= io.rs2Val
        }
      }
    }
  }

}
