package wb_device

import chisel3.util._
import chisel3._
import chisel3.experimental.ChiselEnum
import wb_device.ihex.WBUartIhexWrapper
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}
import wishbone.{WBArbiter, Wishbone}

class TestWBMaster(switchCount: Int = 18, ra: Int = 13, ca: Int = 10, ramdw: Int = 16) extends Module {
  require(switchCount >= 10)
  val io = IO(new Bundle {
    val sdram = new SDRAM(ra=ra, dq=ramdw)
    val dqIn = Input(UInt(ramdw.W))
    val switches = Input(UInt(switchCount.W))
    val write = Input(Bool())
    val output = Output(UInt(32.W))
    val stall = Output(Bool())
    val idle = Output(Bool())
    val tx = Output(Bool())
    val rx = Input(Bool())
  })

  val uart = Module(new WBUartIhexWrapper())
  uart.io.rx := io.rx
  io.tx := uart.io.tx
  uart.io.slaveWb <> DontCare
  uart.io.slaveWb.cyc := false.B
  val thisMaster = Wire(Flipped(new Wishbone()))

  val arb = Module(new WBArbiter)
  arb.io.masters(0) <> thisMaster
  arb.io.masters(1) <> uart.io.masterWb

  val ramController = Module(new WBSDRAMCtlr(ra=ra, ca = ca, dq=ramdw))
  io.sdram := ramController.io.sdram
  ramController.io.dataIn := io.dqIn
  ramController.io.wb <> arb.io.output

  val out = RegInit(0.U(32.W))
  io.output := out
  val switches = RegNext(io.switches)
  val write = RegNext(io.write)

  object WBState extends ChiselEnum {
    val idle, readStrobe, readWait = Value
  }

  val wb = thisMaster
  val state = RegInit(WBState.idle)
  val wbAddr = RegInit(0.U(32.W))
  val writeData = RegInit(0.U(8.W))
  wb.addr := switches(switchCount-2,0)
  wb.cyc := false.B
  wb.we := false.B
  wb.sel := "b1111".U
  wb.mosi_data := DontCare
  wb.stb := false.B

  io.stall := !wb.stall
  io.idle := state === WBState.idle

  switch(state) {
    is(WBState.idle) {
      when(!wb.stall) {
        when(switches(switchCount-1)) {
          state := WBState.readStrobe
        }
      }
    }
    is(WBState.readStrobe) {
      wb.cyc := true.B
      wb.stb := true.B
      when(!wb.stall) {
        state := WBState.readWait
      }
      when(wb.ack) {
        out := wb.miso_data
        state := WBState.idle
      }
    }
    is(WBState.readWait) {
      wb.cyc := true.B
      when(wb.ack) {
        out := wb.miso_data
        state := WBState.idle
      }
    }
  }

}

