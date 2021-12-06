package simple_rv32

import chisel3._
import wb_device.{BlockRam, Memdev, WBLeds, WBNeoPixel, WBSSeg, WBTimer}
import wb_device.ihex.{WBUartIhexWrapper, WBUartWithIhex}
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wb_device.sseg.SSEG
import wishbone.{WBAddressRange, WBArbiter, WBInterconnect, Wishbone}

class FullSystemTop(numSwitches: Int = 18, numLeds: Int = 16, ramDq: Int = 16, simulation: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val sdram = new SDRAM(dq = ramDq)
    val dqIn = Input(UInt(ramDq.W))

    val switches = Input(UInt(numSwitches.W))
    val leds = Output(UInt(numLeds.W))
    val rx = Input(Bool())
    val tx = Output(Bool())

    val neopixel_data = Output(Bool())

    val ssegs = Output(Vec(8, UInt(7.W)))
  })
  dontTouch(io)

  val core = withReset((io.switches(numSwitches - 1) || reset.asBool()))(Module(new ProcTop(fullSystem = true)))

  val leds = Module(new WBLeds)
  leds.io.leds <> io.leds
  leds.io.switches := io.switches(15, 0)

  val ssegs = Module(new WBSSeg(numSSeg = 8))
  io.ssegs := ssegs.io.ssegs

  val ihexUart = Module(new WBUartIhexWrapper())
  ihexUart.io.rx := io.rx
  io.tx := ihexUart.io.tx
  ihexUart.io.slaveWb.cyc := false.B

  val arbiter = Module(new WBArbiter())
  arbiter.io.masters(1) <> core.io.wb
  arbiter.io.masters(0) <> ihexUart.io.masterWb

  val ramWB = if (simulation) {
    io.sdram <> DontCare
    val memdev = Module(new Memdev())
    val theWb = Wire(Flipped(new Wishbone()))
    memdev.io.i_clk := clock
    memdev.io.i_wb_cyc := theWb.cyc
    memdev.io.i_wb_stb := theWb.stb
    memdev.io.i_wb_we := theWb.we
    memdev.io.i_wb_sel := theWb.sel
    memdev.io.i_wb_addr := theWb.addr
    memdev.io.i_wb_data := theWb.mosi_data
    theWb.miso_data := memdev.io.o_wb_data
    theWb.ack := memdev.io.o_wb_ack
    theWb.stall :=  memdev.io.o_wb_stall
    theWb.error := false.B
    theWb
  } else {
//    val blockram = Module(new BlockRam(aw=13))
//    blockram.io.wb
    val dramController = Module(new WBSDRAMCtlr(dq = ramDq))
    io.sdram := dramController.io.sdram
    dramController.io.dataIn := io.dqIn
    dramController.io.wb
  }

  val neopixel = Module(new WBNeoPixel(maxNumLeds = 32))
  io.neopixel_data := neopixel.io.led

  val timer1 = Module(new WBTimer) // Tick 1ms

  val interconnect = Module(new WBInterconnect(Array
  (
    WBAddressRange("RAM", 0x0, 128 * 1024 * 1024),
    WBAddressRange("LEDs", 0xF0000000, 4),
    WBAddressRange("SSEG", 0xF0000004, 4),
    WBAddressRange("NeoPixel", 0xF0002000, 1024),
    WBAddressRange("UART", base_address = 0xF0001000, numAddresses = 8),
    WBAddressRange("Timer", 0xF0000010, 4),
  )))
  interconnect.io.devices(0) <> ramWB
  interconnect.io.devices(1) <> leds.io.wb
  interconnect.io.devices(2) <> ssegs.io.wb
  interconnect.io.devices(3) <> neopixel.io.wb
  interconnect.io.devices(4) <> ihexUart.io.slaveWb
  interconnect.io.devices(5) <> timer1.io.wb

  interconnect.io.master <> arbiter.io.output
}
