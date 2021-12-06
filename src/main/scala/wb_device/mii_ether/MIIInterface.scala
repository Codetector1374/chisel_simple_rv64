package wb_device.mii_ether

import chisel3._

class MIIManagementIf extends Bundle {
  val clk = Output(Bool())
  val dataOut = Output(Bool())
  val outEn = Output(Bool())
  val dataIn = Input(Bool())
}

class MIITx extends Bundle {
  val clk = Input(Clock())
  val data = Output(UInt(4.W))
  val en = Output(Bool())
  val err = Output(Bool())
}

class MIIRx extends Bundle {
  val clk = Input(Clock())
  val data = Input(UInt(4.W))
  val valid = Input(Bool())
  val err = Input(Bool())
  val carrierSense = Input(Bool())
  val collision = Input(Bool())
}

class MIIInterface extends Bundle {
  val tx = new MIITx
  val rx = new MIIRx
  val mgmt = new MIIManagementIf
}

