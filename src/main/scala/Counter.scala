import chisel3._

/** 8비트 카운터.
  *  - en 이 1인 사이클마다 count 가 1 증가
  *  - 255 다음은 자연 오버플로로 0 (별도 wrap 로직 불필요)
  *
  * 참고: docs/07-chisel-hands-on.md §2.2(Module/IO), §2.3(Reg), §2.5(when), §3
  */
class Counter extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val count = Output(UInt(8.W))
  })

  // TODO 1: 8비트 레지스터를 리셋값 0으로 선언한다. (RegInit)
  val cnt = RegInit(0.U(8.W))
  

  // TODO 2: io.en 이 true 인 사이클에만 cnt 를 1 증가시킨다. (when)
  //         else 는 안 써도 레지스터는 값을 유지한다.
   when(io.en){
    cnt := cnt +1.U
  }

  // TODO 3: io.count 에 cnt 를 연결한다.
  io.count := cnt
}
