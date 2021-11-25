package simple_rv32

import chisel3._
import chiseltest.experimental.TestOptionBuilder._
import chisel3.tester._
import chisel3.util.experimental.loadMemoryFromFile
import org.scalatest.FreeSpec

class FetchTest extends FreeSpec with ChiselScalatestTester {
  "stuff" in {
    test(new Fetch).withFlags(Array("writeVcd=1")) { fetch =>
      fetch.clock.step(100)
    }
  }
}
