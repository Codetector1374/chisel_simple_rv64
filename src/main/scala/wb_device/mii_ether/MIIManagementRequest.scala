package wb_device.mii_ether

import chisel3._

class MIIManagementRequest extends Bundle {
  val bWrite = Bool()
  val phyAddr = UInt(5.W)
  val regAddr = UInt(5.W)
  val wrData = UInt(16.W)
}
