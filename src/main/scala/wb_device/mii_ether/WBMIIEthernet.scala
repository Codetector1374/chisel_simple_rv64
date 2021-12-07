package wb_device.mii_ether

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import wishbone.Wishbone

/*
Register Map
Offset 3-0: N/A | N/A | mgmtPhyAddr | mgmtRegAddr
Offset 7-4: N/A | N/A | MgmtData H  | MgmtData L

 */
class WBMIIEthernet(sysClock: Int = 50000000, mgmtClock: Int = 1000000) extends Module {
  val io = IO(new Bundle {
    val wb = new Wishbone()
    val mii = new MIIInterface
  })

  io.mii <> DontCare

  val mgmtUnit = Module(new MIIManagement(sysClock, mgmtClock))
  mgmtUnit.io.mgmt <> io.mii.mgmt
  mgmtUnit.io.request.valid := false.B
  mgmtUnit.io.request.bits := nextMgmtRequest

  object WBState extends ChiselEnum {
    val idle, mgmtIssue, mgmtWait, mgmtAck = Value
  }

  val mgmtPhyAddr = RegInit(0.U(5.W))
  val mgmtRegAddr = RegInit(0.U(5.W))
  val mgmtData = Reg(UInt(16.W))
  val mgmtWrite = Reg(Bool())
  val mgmtResponseData = Reg(UInt(16.W))
  lazy val nextMgmtRequest = Wire(new MIIManagementRequest)
  nextMgmtRequest.regAddr := mgmtRegAddr
  nextMgmtRequest.phyAddr := mgmtPhyAddr
  nextMgmtRequest.bWrite := mgmtWrite
  nextMgmtRequest.wrData := mgmtData

  val state = RegInit(WBState.idle)

  io.wb.ack := false.B
  io.wb.error := false.B
  io.wb.stall := state =/= WBState.idle
  io.wb.miso_data := DontCare
  switch(state) {
    is(WBState.idle) {
      when(io.wb.cyc && io.wb.stb) {
        when(io.wb.addr === 0.U) { // WBOffset 0 : Mgmt Address
          io.wb.ack := true.B
          when(io.wb.we) {
            when(io.wb.sel(0)) {
              mgmtRegAddr := io.wb.mosi_data(4,0)
            }
            when(io.wb.sel(1)) {
              mgmtPhyAddr := io.wb.mosi_data(12,8)
            }
          }.otherwise {
            io.wb.miso_data := Cat(0.U(19.W), mgmtPhyAddr, 0.U(3.W), mgmtRegAddr)
          }
        }.elsewhen(io.wb.addr === 1.U) { // WBOffset 1: Issue Mgmt
          when(io.wb.we) {
            mgmtWrite := true.B
            mgmtData := io.wb.mosi_data(15,0)
          }.otherwise {
            mgmtWrite := false.B
          }
          state := WBState.mgmtIssue
        }
      }
    }
    is(WBState.mgmtIssue) {
      mgmtUnit.io.request.valid := true.B
      when(mgmtUnit.io.request.ready) {
        state := WBState.mgmtWait
      }
    }
    is(WBState.mgmtWait) {
      when(mgmtUnit.io.response.valid) {
        mgmtResponseData := mgmtUnit.io.response.bits.value
        state := WBState.mgmtAck
      }
    }
    is(WBState.mgmtAck) {
      state := WBState.idle
      io.wb.miso_data := Cat(0.U(16.W), mgmtResponseData)
      io.wb.ack := true.B
    }
  }
}

