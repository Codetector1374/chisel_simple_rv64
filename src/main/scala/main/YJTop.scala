package main

import chisel3._
import wb_device.{BlockRam, WBLeds}
import wb_device.ihex.WBUartIhexWrapper
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wishbone.{WBAddressRange, WBArbiter, WBInterconnect}
import yunjue.YJTopWrapper

class YJTop extends Module {
  val io = IO(new Bundle {
    val sdram = new SDRAM(ra = 12)
    val dqIn = Input(UInt(16.W))

    //    val switches = Input(UInt(18.W))
    val leds = Output(UInt(16.W))
    val rx = Input(Bool())
    val tx = Output(Bool())

    val procReset = Input(Bool())
  })
  io <> DontCare
  dontTouch(io)

  val proc = Module(new YJTopWrapper)
  proc.io.procReset := io.procReset

  val leds = Module(new WBLeds)
  leds.io.leds <> io.leds

  val ihexUart = Module(new WBUartIhexWrapper())
  ihexUart.io.rx := io.rx
  io.tx := ihexUart.io.tx
  ihexUart.io.slaveWb.cyc := false.B

//  val dramController = Module(new WBSDRAMCtlr(ra = 12, ca = 8))
//  io.sdram := dramController.io.sdram
//  dramController.io.dataIn := io.dqIn
  io.sdram.output_en := false.B

  val blockram = Module(new BlockRam(aw=13))

  val arbiter = Module(new WBArbiter(numConn = 3))
  arbiter.io.masters(0) <> ihexUart.io.masterWb
  arbiter.io.masters(1) <> proc.io.memWb
  arbiter.io.masters(2) <> proc.io.fetchWb

  val interconnect = Module(new WBInterconnect(Array
  (
    new WBAddressRange("RAM", 0x0, 4 << 13 ),
    new WBAddressRange("LEDs", 0xF0000000, 4),
    new WBAddressRange("UART", base_address = 0xF0001000, numAddresses = 8),
  )))
  interconnect.io.devices(0) <> blockram.io.wb
  interconnect.io.devices(1) <> leds.io.wb
  interconnect.io.devices(2) <> ihexUart.io.slaveWb

  interconnect.io.master <> arbiter.io.output

}

