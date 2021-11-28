package simple_rv32

import chisel3._
import wb_device.WBLeds
import wb_device.ihex.{WBUartIhexWrapper, WBUartWithIhex}
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wishbone.{WBAddressRange, WBInterconnect}

class FullSystemTop(numSwitches: Int = 18, numLeds: Int = 16) extends Module {
  val io = IO(new Bundle {
    val sdram = new SDRAM
    val dqIn = Input(UInt(16.W))

//    val switches = Input(UInt(18.W))
    val leds = Output(UInt(numLeds.W))
    val rx = Input(Bool())
    val tx = Output(Bool())
  })
  io <> DontCare
  dontTouch(io)

//  val core = Module(new ProcTop(fullSystem = true))

  val leds = Module(new WBLeds)
  leds.io.leds <> io.leds

  val ihexUart = Module(new WBUartIhexWrapper())
  ihexUart.io.rx := io.rx
  io.tx := ihexUart.io.tx
  ihexUart.io.slaveWb.cyc := false.B

  val dramController = Module(new WBSDRAMCtlr)
  io.sdram := dramController.io.sdram
  dramController.io.dataIn := io.dqIn

  val interconnect = Module(new WBInterconnect(Array
  (
    new WBAddressRange("RAM", 0x0, 64 * 1024 * 1024 ),
    new WBAddressRange("LEDs", 0xF0000000, 4),
    new WBAddressRange("UART", base_address = 0xF0001000, numAddresses = 8),
  )))
  interconnect.io.devices(0) <> dramController.io.wb
  interconnect.io.devices(1) <> leds.io.wb
  interconnect.io.devices(2) <> ihexUart.io.slaveWb

  interconnect.io.master <> ihexUart.io.masterWb
}
