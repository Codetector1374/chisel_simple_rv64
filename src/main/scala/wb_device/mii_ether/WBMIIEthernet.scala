package wb_device.mii_ether

import chisel3._
import wishbone.Wishbone

class WBMIIEthernet(sysClock: Int = 50000000, mgmtClock: Int = 1000000) extends Module {
  val io = IO(new Bundle {
    val wb = new Wishbone()
    val mii = new MIIInterface
  })



}

