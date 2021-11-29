package wb_device.sseg

import chisel3._
import chisel3.util._

class SSEG(numSSEG: Int = 1) extends Module {
  val io = IO(new Bundle {
    val value = Input(UInt((4 * numSSEG).W))
    val ssegs = Output(Vec(numSSEG, UInt(7.W)))
  })

  def ssegValue(in: UInt): UInt = {
    require(in.getWidth == 4)
    val out = Wire(UInt(7.W))
    out := "b0000000".U
    switch(in) {
      is("h0".U) {
        out := "b1111110".U
      }
      is("h1".U) {
        out := "b0110000".U
      }
      is("h2".U) {
        out := "b1101101".U
      }
      is("h3".U) {
        out := "b1111001".U
      }
      is("h4".U) {
        out := "b0110011".U
      }
      is("h5".U) {
        out := "b1011011".U
      }
      is("h6".U) {
        out := "b1011111".U
      }
      is("h7".U) {
        out := "b1110000".U
      }
      is("h8".U) {
        out := "b1111111".U
      }
      is("h9".U) {
        out := "b1111011".U
      }
      is("ha".U) {
        out := "b1110111".U
      }
      is("hb".U) {
        out := "b0011111".U
      }
      is("hc".U) {
        out := "b1001110".U
      }
      is("hd".U) {
        out := "b0111101".U
      }
      is("he".U) {
        out := "b1001111".U
      }
      is("hf".U) {
        out := "b1000111".U
      }
    }
    out
  }

  for (x <- 0 until numSSEG) {
    io.ssegs(x) := ~ssegValue(io.value(4*(x+1)-1, 4*x))
  }

}

