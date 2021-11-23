package simple_rv64

import chisel3.stage.ChiselStage
import wb_device.Memdev
object Main {
  def main(args: Array[String]): Unit = {
    println(ChiselStage.emitSystemVerilog(new Top))
  }
}