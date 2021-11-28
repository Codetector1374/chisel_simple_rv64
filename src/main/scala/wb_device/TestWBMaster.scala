package wb_device

import chisel3.util._
import chisel3._
import chisel3.experimental.ChiselEnum
import wb_device.sdram.{SDRAM, WBSDRAMCtlr}

class TestWBMaster extends Module {
  val io = IO(new Bundle {
    val sdram = new SDRAM(ra=12)
    val dqIn = Input(UInt(16.W))
    val switches = Input(UInt(18.W))
    val write = Input(Bool())
    val output = Output(UInt(8.W))
    val stall = Output(Bool())
    val idle = Output(Bool())
  })

  val ramController = Module(new WBSDRAMCtlr(ra=12, ca = 8))
  io.sdram := ramController.io.sdram
  ramController.io.dataIn := io.dqIn

  val out = RegInit(0.U(8.W))
  io.output := out
  val switches = RegNext(io.switches)
  val write = RegNext(io.write)

  val wb = ramController.io.wb

  object WBState extends ChiselEnum {
    val idle, readStrobe, readWait, writeStrobe, writeWait = Value
  }

  val state = RegInit(WBState.idle)
  val wbAddr = RegInit(0.U(32.W))
  val writeData = RegInit(0.U(8.W))
  wb.addr := switches(15,8)
  wb.cyc := true.B
  wb.we := false.B
  wb.sel := "b1111".U
  wb.mosi_data := Cat(0.U(4.W), writeData(7,4), 0.U(16.W), writeData(3,0))
  wb.stb := false.B

  io.stall := !wb.stall
  io.idle := state === WBState.idle

  switch(state) {
    is(WBState.idle) {
      when(!wb.stall) {
        when(switches(17)) {
          state := WBState.readStrobe
        }
        when(write) {
          state := WBState.writeStrobe
          writeData := switches(7, 0)
        }
      }
    }
    is(WBState.readStrobe) {
      wb.stb := true.B
      when(!wb.stall) {
        state := WBState.readWait
      }
      when(wb.ack) {
        out := Cat(wb.miso_data(19, 16), wb.miso_data(3, 0))
        state := WBState.idle
      }
    }
    is(WBState.writeStrobe) {
      wb.stb := true.B
      wb.we := true.B
      when(!wb.stall) {
        state := WBState.writeWait
      }
      when(wb.ack) {
        state := WBState.idle
      }
    }
    is(WBState.readWait) {
      when(wb.ack) {
        out := Cat(wb.miso_data(19, 16), wb.miso_data(3, 0))
        state := WBState.idle
      }
    }
    is(WBState.writeWait) {
      when(wb.ack) {
        state := WBState.idle
      }
    }
  }

}

