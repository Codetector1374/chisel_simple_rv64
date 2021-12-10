package wb_device.mii_ether

import chisel3._
import chisel3.experimental.ChiselEnum

class MIIManagementRequest extends Bundle {
  val bWrite = Bool()
  val phyAddr = UInt(5.W)
  val regAddr = UInt(5.W)
  val wrData = UInt(16.W)
}

class MIIManagementResponse extends Bundle {
  val value = UInt(16.W)
}

object MIITxRequestType extends ChiselEnum {
  val pushByte = Value(0.U)
  val send = Value
}

class MIITxRequest extends Bundle {
  val reqType = MIITxRequestType()
  val payload = UInt(8.W)
}

class MIIRxPacketSegment extends Bundle {
  val payload = UInt(4.W)
  val bEnd = Bool()
}

class MIIRxPacketByte extends Bundle {
  val payload = UInt(8.W)
  val bStart = Bool()
  val bEnd = Bool()
}

class MIIRxFrameInfo extends Bundle {
  val valid = Bool()
  val size = UInt(16.W)
}
