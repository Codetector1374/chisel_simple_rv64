package simple_rv32

import chisel3.stage.ChiselStage
import wb_device.Memdev
import wishbone.WBArbiter

import scala.reflect.io.File
object Main {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new Top, args = Array("-td", "generated"))
//    (new ChiselStage).emitVerilog(new WBArbiter(numConn = 4), args= Array("-td", "generated/wbarb"))
  }
}