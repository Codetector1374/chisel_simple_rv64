package simple_rv32

import chisel3._

object RV32Opcode  {
  val NOP   = "b0000000".U
  val LUI   = "b0110111".U
  val AUIPC = "b0010111".U
  val JAL   = "b1101111".U
  val JALR  = "b1100111".U
  val BR    = "b1100011".U
  val LOAD  = "b0000011".U
  val STORE = "b0100011".U
  val ALUI  = "b0010011".U
  val ALU   = "b0110011".U
}

object LsFunc3 {
  val Byte = "b000".U
  val Half = "b001".U
  val Word = "b010".U
  val ByteUnsigned = "b100".U
  val HalfUnsigned = "b101".U
}

object AluFunc3 {
  val ADD_SUB   = "b000".U
  val SLT   = "b010".U
  val SLTU  = "b011".U
  val XOR   = "b100".U
  val OR    = "b110".U
  val AND   = "b111".U
  val SLL   = "b001".U
  val SRx   = "b101".U
}

object AluFunc7 {
  val default = "b0000000".U
  val alt1 = "b0100000".U
}