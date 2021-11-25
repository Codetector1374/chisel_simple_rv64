package wishbone

import chisel3._

class WBArbiter(numConn: Int = 2) extends Module {
  require(numConn >= 2, "can't arbitrate between less than 2 connections")
  val io = IO(new Bundle {
    val masters = Vec(numConn, new Wishbone())
    val output = Flipped(new Wishbone())
  })

  val txnCounters = RegInit(0.U(2.W))
  val currentTxn = Wire(UInt(2.W))

  for (x <- 0 until numConn) {
    io.masters(x).ack := false.B
    io.masters(x).miso_data := DontCare
    io.masters(x).error := false.B
    io.masters(x).stall := true.B
  }

  val currentMaster = RegInit(0.U(8.W))

  val nextMaster = Wire(UInt(8.W))
  nextMaster := 0.U
  for(x <- (numConn - 1) to 0 by -1) {
    when((!io.masters(currentMaster).cyc) || x.U < currentMaster) {
      when(io.masters(x).cyc && io.masters(x).stb) {
        nextMaster := x.U
      }
    }
  }

  currentTxn := txnCounters
  when(io.masters(currentMaster).cyc) {
    when(io.masters(currentMaster).stb) {
      when (io.masters(currentMaster).stall && !io.masters(currentMaster).ack) {
        currentTxn := txnCounters + 1.U
      }
    }.elsewhen(io.masters(currentMaster).ack) {
      currentTxn := txnCounters - 1.U
    }
  }.otherwise {
    currentTxn := 0.U
  }
  txnCounters := currentTxn

  io.output <> io.masters(currentMaster)
  when(nextMaster =/= currentMaster) {
    io.masters(currentMaster).stall := true.B
    when(!io.masters(currentMaster).cyc || currentTxn === 0.U) {
      currentMaster := nextMaster
    }
  }
}

