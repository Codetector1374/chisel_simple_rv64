package wb_device.mii_ether

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import chisel3.util.Queue
import freechips.asyncqueue.{AsyncQueue, AsyncQueueParams}
import wb_device.SinglePortRam

class MIIEthernetRX extends RawModule {
  val io = IO(new Bundle {
    val sysClk = Input(Clock())
    val reset = Input(Bool())

    val frameInfo = Output(new MIIRxFrameInfo)
    val clrFrame = Input(Bool())
    val readAddr = Input(UInt(11.W))
    val data = Output(UInt(8.W))

    val miiRx = new MIIRx
  })


  val cdcRxQ = withClockAndReset(io.sysClk, io.reset)(Module(new AsyncQueue(new MIIRxPacketByte, AsyncQueueParams(depth = 16))))
  cdcRxQ.io.enq_clock := io.miiRx.clk
  cdcRxQ.io.enq_reset := io.reset
  cdcRxQ.io.deq_clock := io.sysClk
  cdcRxQ.io.deq_reset := io.reset

  withClockAndReset(io.miiRx.clk, io.reset) {
    val packetBuffer = Module(new Queue(new MIIRxPacketSegment, 2048, false, false))
    packetBuffer.io.enq.valid := false.B
    packetBuffer.io.enq.bits := DontCare

    // Rx from MII interface
    object MiiRxState extends ChiselEnum {
      val idle, sof_1, rx, error_recovery = Value
    }
    val rxState = RegInit(MiiRxState.idle)
    val rxCntr = Reg(UInt(11.W))

    switch(rxState) {
      is(MiiRxState.idle) {
        when(io.miiRx.valid && io.miiRx.data === "h5".U) { // Preamble or SFD least significant nibble
          rxState := MiiRxState.sof_1
        }
      }
      is(MiiRxState.sof_1) {
        when(io.miiRx.valid) { // SFD Most Significant nibble
          when(io.miiRx.data === "hD".U) { // Next nibble should be stuff
            rxState := MiiRxState.rx
            rxCntr := 0.U
          }.elsewhen(io.miiRx.data =/= "h5".U) { // not Preamble or SFD least significant nibble
            rxState := MiiRxState.idle
          }
        }
      }
      is(MiiRxState.rx) {
        when(packetBuffer.io.enq.ready) {
          packetBuffer.io.enq.valid := true.B
          packetBuffer.io.enq.bits.payload := io.miiRx.data
          when(io.miiRx.valid) {
            packetBuffer.io.enq.bits.bEnd := false.B
          }.otherwise {
            packetBuffer.io.enq.bits.bEnd := true.B
            rxState := MiiRxState.idle
          }
        }.otherwise {
          rxState := MiiRxState.error_recovery
        }
      }
      is(MiiRxState.error_recovery) {
        when(packetBuffer.io.enq.ready) {
          packetBuffer.io.enq.bits.bEnd := true.B
          packetBuffer.io.enq.valid := true.B
          rxState := MiiRxState.idle
        }
      }
    }

    // Dequeue from buffer and push into the other clock domain
    object RxDeqState extends ChiselEnum {
      val idle, rx = Value
    }
    val deqState = RegInit(RxDeqState.idle)
    val lsbBuffer = RegInit(0.U.asTypeOf(Valid(UInt(4.W))))
    val bStart = RegInit(false.B)

    val deqData = packetBuffer.io.deq.bits
    val cdcEnq = cdcRxQ.io.enq
    cdcEnq.valid := false.B
    cdcEnq.bits.bEnd := false.B
    cdcEnq.bits.bStart := false.B
    cdcEnq.bits.payload := DontCare

    packetBuffer.io.deq.ready := true.B
    switch(deqState) {
      is(RxDeqState.idle) {
        when(packetBuffer.io.deq.valid) {
          when(!deqData.bEnd) {
            lsbBuffer.bits := deqData.payload
            lsbBuffer.valid := true.B
            deqState := RxDeqState.rx
            bStart := true.B
          }
        }
      }
      is(RxDeqState.rx) {
        when(!deqData.bEnd) {
          when(lsbBuffer.valid) { // this is the second nibble
            when(cdcEnq.ready) {
              // Push into the CDC FIFO
              cdcEnq.valid := true.B
              cdcEnq.bits.bStart := bStart
              cdcEnq.bits.payload := Cat(deqData.payload, lsbBuffer.bits)

              // Clear first byte flag
              bStart := false.B

              lsbBuffer.valid := false.B
            }.otherwise { // if we can't push we wait
              packetBuffer.io.deq.ready := false.B
            }
          }.otherwise { // this is the lower nibble
            lsbBuffer.valid := true.B
            lsbBuffer.bits := deqData.payload
          }
        }.otherwise { // deqData.bEnd
          packetBuffer.io.deq.ready := cdcEnq.ready
          cdcEnq.valid := true.B
          cdcEnq.bits.bEnd := true.B
          when(cdcEnq.ready) {
            deqState := RxDeqState.idle
          }
        }
      }
    }
  }

  withClockAndReset(io.sysClk, io.reset) {
    val cdcDeq = cdcRxQ.io.deq
    val frameInfo = RegInit(0.U.asTypeOf(new MIIRxFrameInfo))
    io.frameInfo := frameInfo
    val frameMem = Module(new SinglePortRam(aw = 11, dw = 8))
    frameMem.io.clock := io.sysClk
    io.data := frameMem.io.data_out
    frameMem.io.we := false.B
    frameMem.io.data_in := DontCare
    when(frameInfo.valid) {
      frameMem.io.addr := io.readAddr
    }.otherwise {
      frameMem.io.addr := frameInfo.size
    }


    when(io.clrFrame && frameInfo.valid) {
      frameInfo.valid := false.B
      frameInfo.size := 0.U
    }

    object SysDeqState extends ChiselEnum {
      val idle, deq = Value
    }
    val state = RegInit(SysDeqState.idle)

    cdcDeq.ready := false.B
    when(!frameInfo.valid && cdcDeq.valid) {
      cdcDeq.ready := true.B
      switch(state) {
        is(SysDeqState.idle) {
          when(cdcDeq.bits.bStart) {
            frameMem.io.we := true.B
            frameMem.io.data_in := cdcDeq.bits.payload
            frameInfo.size := frameInfo.size + 1.U
            state := SysDeqState.deq
          }
        }
        is(SysDeqState.deq) {
          when(!cdcDeq.bits.bEnd) {
            frameMem.io.we := true.B
            frameMem.io.data_in := cdcDeq.bits.payload
            frameInfo.size := frameInfo.size + 1.U
          }.otherwise {
            // TODO Validate Frame
            frameInfo.valid := true.B
          }
        }
      }
    }

  }

}

