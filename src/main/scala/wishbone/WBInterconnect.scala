package wishbone

import chisel3._
import math.{pow, ceil, log}

case class WBAddressRange
(
name: String,
base_address: Int,
numAddresses: Int
) {
  require(numAddresses % 4 == 0, "WB require a multiple of 4")
  val activeBits = ceil(log(numAddresses) / log(2)).toInt
  val baseMask = (1 << activeBits) - 1
  require((base_address & baseMask) == 0)
  val limit_address = (base_address | baseMask).toInt
  val wb_base = (base_address >>> activeBits)
}

class WBInterconnect(addrMap: Array[WBAddressRange]) extends Module {

  println("=============== WBInterconnect ADDRESS MAP ================")
  for(x <- addrMap) {
    println(f"0x${x.base_address}%08X - 0x${x.limit_address}%08X activeBits: ${x.activeBits}=> ${x.name}")
  }
  println("=============== END ADDRESS MAP ===========================")

  val io = IO(new Bundle {
    val devices = Vec(addrMap.length, Flipped(new Wishbone()))
    val master = new Wishbone
  })

  val wback = RegInit(false.B)
  val wbdata = Reg(UInt(32.W))
  wbdata := 0.U
  io.master.miso_data := wbdata
  io.master.ack := wback
  val wberr = RegInit(false.B)
  wberr := false.B
  io.master.error := wberr

  val wbAcks = Wire(Vec(addrMap.length, Bool()))
  wback := wbAcks.asUInt().orR()
  for(x <- addrMap.indices) {
    wbAcks(x) := io.devices(x).ack
    when(io.devices(x).ack) {
      wbdata := io.devices(x).miso_data
    }
  }


  val deviceSels = Wire(Vec(addrMap.length, Bool()))
  for(x <- addrMap.indices) {
    val dev = addrMap(x)
    deviceSels(x) := io.master.addr(29, 0) >= (dev.base_address >>> 2).U && io.master.addr(29,0) <= (dev.limit_address >>> 2).U
  }
  when(io.master.stb && !deviceSels.asUInt().orR()) {
    wberr := true.B
  }

  val stalls = Wire(Vec(addrMap.length, Bool()))
  for(x <- addrMap.indices) {
    stalls(x) := deviceSels(x) && io.devices(x).stall
  }
  io.master.stall := stalls.asUInt().orR()

  for(x <- addrMap.indices) {
    val dev = addrMap(x)
    io.devices(x).stb := io.master.stb && deviceSels(x)
    io.devices(x).sel := io.master.sel
    if (dev.activeBits > 3) {
      io.devices(x).addr := io.master.addr(dev.activeBits - 2 - 1, 0).asUInt()
    } else if (dev.activeBits > 2) {
      io.devices(x).addr := io.master.addr(0).asUInt()
    } else {
      io.devices(x).addr := 0.U
    }
    io.devices(x).we := io.master.we
    io.devices(x).cyc := io.master.cyc && deviceSels(x)
    io.devices(x).mosi_data := io.master.mosi_data
  }

}

