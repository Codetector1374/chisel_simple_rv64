package simple_rv64

import chisel3.stage.ChiselStage
import gcd.GCD
object Main {
  def main(args: Array[String]): Unit = {
    println(ChiselStage.emitSystemVerilog(new Fetch()))
  }
}