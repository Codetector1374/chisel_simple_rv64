package simple_rv32.ex

import chisel3._
import chisel3.util._
import simple_rv32.{AluFunc3, AluFunc7, RV32Opcode}

class ALU extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val pc = Input(UInt(32.W))
    val func7 = Input(UInt(7.W))
    val func3 = Input(UInt(3.W))
    val rs1Val = Input(UInt(32.W))
    val rs2Val = Input(UInt(32.W))
    val imm = Input(UInt(32.W))

    val result = Output(UInt(32.W))
  })

  val opcode = io.opcode
  val result = io.result

  val aluSecondArg = Wire(UInt(32.W))
  when(opcode === RV32Opcode.ALUI) {
    aluSecondArg := io.imm
  } .otherwise {
    aluSecondArg := io.rs2Val
  }

  result := DontCare
  when(opcode === RV32Opcode.ALU || opcode === RV32Opcode.ALUI) {
    switch(io.func3) {
      is(AluFunc3.ADD_SUB) {
        when(opcode === RV32Opcode.ALU && io.func7 === AluFunc7.alt1) {
          result := (io.rs1Val.asSInt() - aluSecondArg.asSInt()).asUInt()
        }.otherwise {
          result := io.rs1Val + aluSecondArg
        }
      }
      is(AluFunc3.SLT) {
        when(io.rs1Val.asSInt() < aluSecondArg.asSInt()) {
          result := 1.U
        } .otherwise {
          result := 0.U
        }
      }
      is(AluFunc3.SLTU) {
        when(io.rs1Val < aluSecondArg) {
          result := 1.U
        } .otherwise {
          result := 0.U
        }
      }
      is(AluFunc3.XOR) {
        result := io.rs1Val ^ aluSecondArg
      }
      is(AluFunc3.OR) {
        result := io.rs1Val | aluSecondArg
      }
      is(AluFunc3.AND) {
        result := io.rs1Val & aluSecondArg
      }
      is(AluFunc3.SLL) {
        result := io.rs1Val << aluSecondArg(4,0)
      }
      is(AluFunc3.SRx) {
        when(io.func7 === AluFunc7.alt1) {
          result := (io.rs1Val.asSInt() >> aluSecondArg(4,0)).asUInt()
        } .otherwise {
          result := io.rs1Val >> aluSecondArg(4,0)
        }
      }
    }
  } .elsewhen(opcode === RV32Opcode.LUI) {
    result := io.imm
  } .elsewhen(opcode === RV32Opcode.AUIPC) {
    result := io.pc + io.imm
  }

}

