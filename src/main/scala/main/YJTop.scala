package main

import chisel3._
import wb_device.{BlockRam, WBLeds, WBSSeg}
import wb_device.ihex.WBUartIhexWrapper
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wishbone.{WBAddressRange, WBArbiter, WBInterconnect}
import yunjue.YJTopWrapper

class YJTop extends Module {
  val io = IO(new Bundle {
    val sdram = new SDRAM(ra = 12)
    val dqIn = Input(UInt(16.W))

    val switches = Input(UInt(18.W))
    val leds = Output(UInt(16.W))
    val rx = Input(Bool())
    val tx = Output(Bool())

    val ssegs = Output(Vec(8, UInt(7.W)))


    val procReset = Input(Bool())
  })
  io <> DontCare
  dontTouch(io)

  val proc = Module(new YJTopWrapper)
  proc.io.procReset := io.procReset

  val leds = Module(new WBLeds)
  leds.io.leds <> io.leds
  leds.io.switches <> io.switches(15, 0).asUInt()

  val ssegs = Module(new WBSSeg(numSSeg = 8))
  io.ssegs := ssegs.io.ssegs

  val ihexUart = Module(new WBUartIhexWrapper())
  ihexUart.io.rx := io.rx
  io.tx := ihexUart.io.tx
  ihexUart.io.slaveWb.cyc := false.B

  val dramController = Module(new WBSDRAMCtlr(ra = 12, ca = 8))
  io.sdram <> dramController.io.sdram
  dramController.io.dataIn := io.dqIn

  val procArbiter = Module(new WBArbiter)
  procArbiter.io.masters(0) <> proc.io.memWb
  procArbiter.io.masters(1) <> proc.io.fetchWb

  val mainArbiter = Module(new WBArbiter)
  mainArbiter.io.masters(0) <> ihexUart.io.masterWb
  mainArbiter.io.masters(1) <> procArbiter.io.output

  val interconnect = Module(new WBInterconnect(Array
  (
    WBAddressRange("RAM", 0x0, 8 * 1024 * 1024 ),
    WBAddressRange("LEDs", 0xF0000000, 4),
    WBAddressRange("HEXs", 0xF0000004, 4),
    WBAddressRange("UART", base_address = 0xF0001000, numAddresses = 8),
  )))
  interconnect.io.devices(0) <> dramController.io.wb
  interconnect.io.devices(1) <> leds.io.wb
  interconnect.io.devices(2) <> ssegs.io.wb
  interconnect.io.devices(3) <> ihexUart.io.slaveWb

  interconnect.io.master <> mainArbiter.io.output

}

