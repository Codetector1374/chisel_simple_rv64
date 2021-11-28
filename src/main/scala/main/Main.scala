package main

import chisel3.stage.ChiselStage
import simple_rv32.{FullSystemTop, ProcTop}
import wb_device.TestWBMaster
import wb_device.sdram.WBSDRAMCtlr

object Main {
  def main(args: Array[String]): Unit = {
    (new ChiselStage).emitVerilog(new ProcTop, args = Array("-td", "generated"))
    (new ChiselStage).emitVerilog(new WBSDRAMCtlr, args = Array("-td", "generated/sdram"))
    (new ChiselStage).emitVerilog(new TestWBMaster, args = Array("-td", "generated/fpga_test"))
    (new ChiselStage).emitVerilog(new FullSystemTop, args = Array("-td", "generated/riscv_full"))
    (new ChiselStage).emitVerilog(new YJTop, args = Array("-td", "generated/yj"))
    //    (new ChiselStage).emitVerilog(new WBArbiter(numConn = 4), args= Array("-td", "generated/wbarb"))
  }
}
