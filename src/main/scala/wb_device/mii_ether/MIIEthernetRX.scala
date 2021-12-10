package wb_device.mii_ether

import chisel3._

class MIIEthernetRX extends RawModule {
  val io = IO(new Bundle {
    val sysClk = Input(Clock())
    val reset = Input(Bool())

    val miiRx = new MIIRx
  })
}

