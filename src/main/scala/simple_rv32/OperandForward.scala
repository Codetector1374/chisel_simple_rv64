package simple_rv32

import chisel3._

class OperandForward extends Bundle {
  val regNo = Input(UInt(5.W))
}
