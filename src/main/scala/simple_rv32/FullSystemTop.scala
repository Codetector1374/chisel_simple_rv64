package simple_rv32

import chisel3._
import wb_device.{BlockRam, Memdev, WBLeds, WBNeoPixel, WBSSeg, WBTimer}
import wb_device.ihex.{WBUartIhexWrapper, WBUartWithIhex}
import wb_device.mii_ether.{MIIInterface, WBMIIEthernet}
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wb_device.sseg.SSEG
import wishbone.{WBAddressRange, WBArbiter, WBInterconnect, Wishbone}

class FullSystemTop(numSwitches: Int = 18, numLeds: Int = 16, ramDq: Int = 16, simulation: Boolean = false) extends Module {
  val sysClockSpeed = 50000000
  val io = IO(new Bundle {
    val sdram = new SDRAM(dq = ramDq)
    val dqIn = Input(UInt(ramDq.W))

    val switches = Input(UInt(numSwitches.W))
    val leds = Output(UInt(numLeds.W))
    val rx = Input(Bool())
    val tx = Output(Bool())

    val neopixel_data = Output(Bool())
    val miiEther0 = new MIIInterface
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
    theWb.stall := memdev.io.o_wb_stall
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

  val miiEthernet = Module(new WBMIIEthernet(sysClock = sysClockSpeed))
  miiEthernet.io.mii <> io.miiEther0

  val interconnect = WBInterconnect(Array
  (
    (WBAddressRange("RAM", 0x0, 128 * 1024 * 1024), ramWB),
    (WBAddressRange("LEDs", 0xF0000000, 4), leds.io.wb),
    (WBAddressRange("SSEG", 0xF0000004, 4), ssegs.io.wb),
    (WBAddressRange("Timer", 0xF0000010, 4), timer1.io.wb),
    (WBAddressRange("UART", 0xF0001000, numAddresses = 8), ihexUart.io.slaveWb),
    (WBAddressRange("NeoPixel", 0xF0002000, 1024), neopixel.io.wb),
    (WBAddressRange("MIIEther", 0xF1000000, 32), miiEthernet.io.wb),
  ))

  interconnect.io.master <> arbiter.io.output
}
