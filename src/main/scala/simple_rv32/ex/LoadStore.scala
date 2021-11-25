package simple_rv32.ex

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import simple_rv32.{LsFunc3, RV32Opcode}
import wishbone.Wishbone


class LoadStore extends Module {

  object WBState extends ChiselEnum {
    val idle, strobe, waitAck = Value
  }

  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val func3 = Input(UInt(3.W))
    val rs1Val = Input(UInt(32.W))
    val rs2Val = Input(UInt(32.W))
    val imm = Input(UInt(32.W))

    val wb = Flipped(new Wishbone)

    val result = Output(UInt(32.W))
    val busy = Output(Bool())
  })

  val isLoad = Wire(Bool())
  val isStore = Wire(Bool())
  val lsTargetAddr = Wire(UInt(32.W))
  val lsTargetAddrOffset = Wire(UInt(2.W))
  val addrValid = Wire(Bool())
  val loadResult = Wire(UInt(32.W))
  isLoad := io.opcode === RV32Opcode.LOAD
  isStore := io.opcode === RV32Opcode.STORE
  lsTargetAddr := io.rs1Val + io.imm
  lsTargetAddrOffset := lsTargetAddr(1, 0)

  val state = RegInit(WBState.idle)

  io.wb <> DontCare
  io.wb.addr := lsTargetAddr(31, 2)
  when (isLoad) {
    io.wb.sel := "b1111".U
  } .elsewhen (isStore) {
    switch(io.func3) {
      is(LsFunc3.Byte) {
        io.wb.sel := ("b0001".U << lsTargetAddrOffset).asUInt()
      }
      is(LsFunc3.Half) {
        io.wb.sel := ("b0011".U << lsTargetAddrOffset).asUInt()
      }
      is(LsFunc3.Word) {
        io.wb.sel := "b1111".U
      }
    }
  }
  when(isStore) {
    io.wb.mosi_data := (io.rs2Val << (lsTargetAddrOffset * 8.U)).asUInt()
  }

  // addr validation
  addrValid := true.B
  when(isLoad || isStore) {
    switch(io.func3) {
      // byte is always valid
      is(LsFunc3.Half, LsFunc3.HalfUnsigned) {
        addrValid := lsTargetAddrOffset(0) === 0.U
      }
      is(LsFunc3.Word) {
        addrValid := lsTargetAddrOffset === 0.U
      }
    }
  }

  // Load Result
  loadResult := DontCare
  val iWbData = io.wb.miso_data >> (lsTargetAddrOffset * 8.U)
  when(isLoad) {
    switch(io.func3) {
      is(LsFunc3.Word) {
        loadResult := iWbData
      }
      is(LsFunc3.Half) {
        loadResult := Cat(Fill(16, iWbData(15)), iWbData(15, 0))
      }
      is(LsFunc3.HalfUnsigned) {
        loadResult := Cat(0.U(16.W), iWbData(15,0))
      }
      is(LsFunc3.Byte) {
        loadResult := Cat(Fill(24, iWbData(7)), iWbData(7, 0))
      }
      is(LsFunc3.ByteUnsigned) {
        loadResult := Cat(0.U(24.W), iWbData(7,0))
      }
    }
  }

  io.busy := false.B
  io.result := DontCare
  io.wb.stb := false.B
  io.wb.cyc := false.B
  io.wb.we := isStore
  when(state === WBState.idle) {
    when(isLoad || isStore) {
      io.busy := true.B

      io.wb.cyc := true.B
      io.wb.stb := true.B

      when(io.wb.ack) {
        io.busy := false.B
        io.result := loadResult
      }.elsewhen(io.wb.error) {
        io.busy := false.B
        io.result := "hDEAD_BEEF".U
      }.elsewhen(!io.wb.stall) {
        state := WBState.waitAck
      }
    }
  }.elsewhen(state === WBState.waitAck) {
    io.busy := true.B
    io.wb.cyc := true.B
    when(io.wb.ack) {
      io.busy := false.B
      io.result := loadResult
      state := WBState.idle
    }.elsewhen(io.wb.error) {
      io.busy := false.B
      io.result := "hDEAD_BEEF".U
      state := WBState.idle
    }
  }

}

