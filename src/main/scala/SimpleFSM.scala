import chisel3._
import chisel3.util._

/** IDLE/READ/WRITE 3상태 FSM.
  *  - Idle + start → rw면 Write, 아니면 Read
  *  - Read/Write → 1사이클 후 Idle
  *  - done: 순수 Moore, state가 Read/Write인 사이클에만 1 (1사이클 펄스)
  *
  * 참고: docs/d06-fsm-and-sv.md §1, docs/d05-chisel-basics-and-counter.md §2.9
  */
object St extends ChiselEnum { val Idle, Read, Write = Value }

class SimpleFSM extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val rw = Input(Bool())
    val done  = Output(Bool())
    val state = Output(St())
  })

  val state = RegInit(St.Idle)

  switch (state) {
    is (St.Idle) {
      when (io.start) {
        state := Mux(io.rw, St.Write, St.Read)
      }
    }

    is (St.Read, St.Write) {
      state := St.Idle
    }
  }

  io.done  := (state === St.Read) || (state === St.Write)
  io.state := state
}
