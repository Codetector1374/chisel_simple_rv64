package wb_device

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import wishbone.Wishbone

class BlockRam(aw: Int = 14) extends Module {
  val io = IO(new Bundle {
    val wb = new Wishbone
  })

  val rams = for (i <- 0 until 4) yield {
    Module(new SinglePortRam(aw = aw))
  }

  object S extends ChiselEnum {
    val IDLE, ACK = Value
  }

  val state = RegInit(S.IDLE)

  io.wb.stall := state =/= S.IDLE
  io.wb.ack := state === S.ACK

  for(x <- rams.indices) {
    val ram = rams(x)
    ram.io.clock := clock
    ram.io.we := io.wb.we && state === S.IDLE && io.wb.stb && io.wb.cyc && io.wb.sel(x).asBool()
    ram.io.addr := io.wb.addr
    ram.io.data_in := io.wb.mosi_data(8 * (x + 1) - 1, 8 * x)
  }

  io.wb.miso_data := Cat(rams(3).io.data_out, rams(2).io.data_out, rams(1).io.data_out, rams(0).io.data_out)
  io.wb.error := false.B

  when(state === S.IDLE) {
    when(io.wb.stb && io.wb.cyc) {
      state := S.ACK
    }
  }
  when(state === S.ACK) {
    state := S.IDLE
  }

}

