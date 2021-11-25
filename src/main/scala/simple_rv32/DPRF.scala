package simple_rv32

import chisel3._
import firrtl.passes.memlib.ReadPort

class RegFileReadPort(width: Int = 32, indexWidth: Int = 5) extends Bundle {
  val regNo = Input(UInt(indexWidth.W))
  val data = Output(UInt(width.W))
}

class RegFileWritePort(width: Int = 32, indexWidth: Int = 5) extends Bundle {
  val regNo = Input(UInt(indexWidth.W))
  val we = Input(Bool())
  val data = Input(UInt(width.W))
}

class DPRF(width: Int = 32, indexWidth: Int = 5, numRegs: Int = 32, zeroReg: Boolean = true) extends Module {
  require(width > 0)
  require(numRegs > 0)
  require(math.pow(2, indexWidth) >= numRegs, s"indexWidth is not large enough to support $numRegs registers")

  val io = IO(new Bundle {
    val read1 = new RegFileReadPort
    val read2 = new RegFileReadPort
    val write = new RegFileWritePort
  })

  val registers = Reg(Vec(numRegs, UInt(width.W)))
  io.read1.data := registers(io.read1.regNo)
  io.read2.data := registers(io.read2.regNo)
  if (zeroReg) {
    when(io.read1.regNo === 0.U) {
      io.read1.data := 0.U
    }
    when(io.read2.regNo === 0.U) {
      io.read2.data := 0.U
    }
  }

  when(io.write.we) {
    registers(io.write.regNo) := io.write.data
  }

  dontTouch(registers)

}
