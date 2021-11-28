package yunjue

import chisel3._

class top extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Reset())

    val wb2_cyc = Output(Bool())
    val wb2_stb = Output(Bool())
    val wb2_we = Output(Bool())
    val wb2_sel = Output(UInt(4.W))
    val wb2_stall = Input(Bool())
    val wb2_ack = Input(Bool())
    val wb2_addr = Output(UInt(30.W))
    val wb2_mosi_data = Output(UInt(32.W))
    val wb2_miso_data = Input(UInt(32.W))

    val wb_cyc = Output(Bool())
    val wb_stb = Output(Bool())
    val wb_we = Output(Bool())
    val wb_sel = Output(UInt(4.W))
    val wb_stall = Input(Bool())
    val wb_ack = Input(Bool())
    val wb_addr = Output(UInt(30.W))
    val wb_mosi_data = Output(UInt(32.W))
    val wb_miso_data = Input(UInt(32.W))
  })

}

