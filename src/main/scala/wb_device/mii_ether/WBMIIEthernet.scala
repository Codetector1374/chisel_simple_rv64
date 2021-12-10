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

  // Power On Reset
  val porCounter = RegInit("hFFFF".U(16.W))
  when(porCounter > 0.U) {
    porCounter := porCounter - 1.U
  }

  io.mii <> DontCare

  val txUnit = Module(new MIIEthernetTX)
  txUnit.io.reset := porCounter.orR()
  txUnit.io.sysClk := clock
  io.mii.tx <> txUnit.io.miiTx
  val txEnq = txUnit.io.reqEnq
  txEnq.valid := false.B
  txEnq.bits <> DontCare

  val rxUnit = Module(new MIIEthernetRX)
  rxUnit.io.reset := porCounter.orR()
  rxUnit.io.sysClk := clock
  rxUnit.io.miiRx <> io.mii.rx
  rxUnit.io.clrFrame := false.B

  val rxReadAddr = RegInit(0.U(11.W))
  rxUnit.io.readAddr := rxReadAddr


  val mgmtUnit = Module(new MIIManagement(sysClock, mgmtClock))
  mgmtUnit.io.mgmt <> io.mii.mgmt
  mgmtUnit.io.request.valid := false.B
  mgmtUnit.io.request.bits := nextMgmtRequest

  object WBState extends ChiselEnum {
    val idle, mgmtIssue, mgmtWait, mgmtAck, txReqWait = Value
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

  val txReqBuffer = Reg(new MIITxRequest)

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
              mgmtRegAddr := io.wb.mosi_data(4, 0)
            }
            when(io.wb.sel(1)) {
              mgmtPhyAddr := io.wb.mosi_data(12, 8)
            }
          }.otherwise {
            io.wb.miso_data := Cat(0.U(19.W), mgmtPhyAddr, 0.U(3.W), mgmtRegAddr)
          }
        }.elsewhen(io.wb.addr === 1.U) { // WBOffset 1: Issue Mgmt
          when(io.wb.we) {
            mgmtWrite := true.B
            mgmtData := io.wb.mosi_data(15, 0)
          }.otherwise {
            mgmtWrite := false.B
          }
          state := WBState.mgmtIssue
        }.elsewhen(io.wb.addr === 2.U) { // WBOffset 2: W: PushByte
          io.wb.ack := true.B
          when(io.wb.we && io.wb.sel(0)) {
            txReqBuffer.payload := io.wb.mosi_data(7, 0)
            txReqBuffer.reqType := MIITxRequestType.pushByte
            txEnq.valid := true.B
            txEnq.bits.payload := io.wb.mosi_data(7, 0)
            txEnq.bits.reqType := MIITxRequestType.pushByte
            when(!txEnq.ready) {
              io.wb.ack := false.B
              state := WBState.txReqWait
            }
          }
        }.elsewhen(io.wb.addr === 3.U) { // WBOffset 3: W: TxSend, R: bTxBusy
          io.wb.ack := true.B
          when(io.wb.we) {
            txReqBuffer.reqType := MIITxRequestType.send
            txEnq.valid := true.B
            txEnq.bits.reqType := MIITxRequestType.send
            when(!txEnq.ready) {
              io.wb.ack := false.B
              state := WBState.txReqWait
            }
          }.otherwise {
            io.wb.miso_data := txUnit.io.txBusy.asUInt()
          }
        }.elsewhen(io.wb.addr === 4.U) { // WBOffset 4: R RxFrameInfo: (frameSize{16}|........|.......bValid{1})
          io.wb.ack := true.B
          when(io.wb.we) {
            rxUnit.io.clrFrame := true.B
          }
          io.wb.miso_data := Cat(rxUnit.io.frameInfo.size, 0.U(15.W), rxUnit.io.frameInfo.valid.asUInt())
        }.elsewhen(io.wb.addr === 5.U) { // WBOffset 5: W: Set RxAddress, R: get a byte and increment address
          io.wb.ack := true.B
          when(io.wb.we && io.wb.sel(0) && io.wb.sel(1)) {
            rxReadAddr := io.wb.mosi_data(10, 0)
          }.otherwise {
            rxReadAddr := rxReadAddr + 1.U
          }
          io.wb.miso_data := rxUnit.io.data
        }.otherwise {
          io.wb.ack := true.B
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
    is(WBState.txReqWait) {
      txEnq.valid := true.B
      txEnq.bits := txReqBuffer
      when(txEnq.ready) {
        io.wb.ack := true.B
        state := WBState.idle
      }
    }
  }
}

