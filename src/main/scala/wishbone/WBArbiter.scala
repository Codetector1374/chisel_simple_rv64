package wishbone

import chisel3._

class WBArbiter(numConn: Int = 2) extends Module {
  require(numConn == 2, "can't arbitrate between less than 2 connections")
//  require(numConn <= 16)
  val io = IO(new Bundle {
    val masters = Vec(numConn, new Wishbone())
    val output = Flipped(new Wishbone())
  })

//  for (x <- 0 until numConn) {
//    io.masters(x).ack := false.B
//    io.masters(x).miso_data := DontCare
//    io.masters(x).error := false.B
//    io.masters(x).stall := true.B
//  }
//
//  val currentMaster = RegInit(0.U(4.W))
//
//  val nextMaster = Wire(UInt(4.W))
//  nextMaster := 0.U
//  for(x <- (numConn - 1) to 1 by -1) {
//    when(io.masters(x).cyc && io.masters(x).stb) {
//      nextMaster := x.U
//    }
//  }
//
//  io.output <> io.masters(currentMaster)
//  when(!io.masters(currentMaster).cyc) {
//    currentMaster := nextMaster
//  }
  val aOwn = RegInit(true.B)

  when(!io.masters(1).cyc) {
    aOwn := true.B
  }.elsewhen(io.masters(1).cyc && io.masters(1).stb && !io.masters(0).cyc) {
    aOwn := false.B
  }

  io.masters(0).miso_data := io.output.miso_data
  io.masters(1).miso_data := io.output.miso_data

  when(aOwn) {
    io.output <> io.masters(0)
    io.masters(1).stall := true.B
    io.masters(1).ack := false.B
    io.masters(1).error := false.B
  } .otherwise {
    io.output <> io.masters(1)
    io.masters(0).stall := true.B
    io.masters(0).ack := false.B
    io.masters(0).error := false.B
  }

}

