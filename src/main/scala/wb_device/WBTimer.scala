package wb_device

import chisel3._
import wishbone.Wishbone

class WBTimer(clockFreq: Int = 50000000, targetTickInterval: Double = 0.001) extends Module {

  val divider = (clockFreq * targetTickInterval).round

  val io = IO(new Bundle {
    val wb = new Wishbone
  })

  val divCounter = RegInit(0.U(32.W))
  val counter = RegInit(0.U(32.W))

  divCounter := divCounter - 1.U
  when(divCounter === 0.U) {
    divCounter := (divider - 1).U
    counter := counter + 1.U
  }

  io.wb.ack := false.B
  io.wb.stall := false.B
  io.wb.error := false.B
  io.wb.miso_data := counter
  when(io.wb.cyc && io.wb.stb) {
    io.wb.ack := true.B
    when(io.wb.we) {
      counter := 0.U
      divCounter := (divider - 1).U
    }
  }
}

