package wb_device.ihex

import chisel3._
import chisel3.util.HasBlackBoxResource

class WBUartWithIhex(clockFreq: Long = 50000000, baud: Long = 115200) extends BlackBox(Map(
  "I_CLOCK_FREQ" -> clockFreq,
  "BAUD_RATE" -> baud
)) with HasBlackBoxResource {
  val io = IO(new Bundle {
    val i_clk = Input(Clock())
    val i_reset = Input(Reset())

    val i_rx = Input(Bool())
    val o_tx = Output(Bool())

    val o_mwb_cyc = Output(Bool())
    val o_mwb_stb = Output(Bool())
    val o_mwb_we = Output(Bool())
    val o_mwb_sel = Output(UInt(4.W))
    val i_mwb_stall = Input(Bool())
    val i_mwb_ack = Input(Bool())
    val i_mwb_err = Input(Bool())
    val o_mwb_addr = Output(UInt(30.W))
    val o_mwb_data = Output(UInt(32.W))
    val i_mwb_data = Input(UInt(32.W))

    val i_wb_cyc = Input(Bool())
    val i_wb_stb = Input(Bool())
    val i_wb_we = Input(Bool())
    val i_wb_sel = Input(UInt(4.W))
    val o_wb_stall = Output(Bool())
    val o_wb_ack = Output(Bool())
    val o_wb_err = Output(Bool())
    val i_wb_addr = Input(UInt(30.W))
    val i_wb_data = Input(UInt(32.W))
    val o_wb_data = Output(UInt(32.W))
  })

  addResource("/ihex/ihex.sv")
  addResource("/ihex/uart_rx.sv")
  addResource("/ihex/uart_tx.sv")
  addResource("/ihex/wb_master_breakout.sv")
  addResource("/ihex/wishbone.sv")
  addResource("/ihex/wbuart_with_ihex.sv")
  addResource("/ihex/wbuart_with_buffer.sv")
}

