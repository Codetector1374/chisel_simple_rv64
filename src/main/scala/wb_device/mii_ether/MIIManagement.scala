package wb_device.mii_ether

import chisel3._

class MIIManagement(sysClk: Int = 50000000, mdc: Int = 1000000) extends Module {
  val clkDiv = math.max(1, (sysClk / mdc) / 2)
  println(s"MII Management interface effective mdc clock: ${sysClk / clkDiv / 2.0}")

  val io = IO(new Bundle {
    val mgmt = new MIIManagementIf
  })

  val clkDivCounter = RegInit((clkDiv-1).U(24.W))
  val mgmtClk = RegInit(false.B)
  clkDivCounter := clkDivCounter - 1.U
  when(clkDivCounter === 0.U) {
    clkDivCounter := (clkDiv-1).U
    mgmtClk := !mgmtClk
  }


}

