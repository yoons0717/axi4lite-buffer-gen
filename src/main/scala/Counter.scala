import chisel3._

/** 8비트 카운터.
  *  - en 이 1인 사이클마다 count 가 1 증가
  *  - 255 다음은 자연 오버플로로 0 (별도 wrap 로직 불필요)
  */
class Counter extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val count = Output(UInt(8.W))
  })

  val cnt = RegInit(0.U(8.W))

   when(io.en){
    cnt := cnt +1.U
  }

  io.count := cnt
}
