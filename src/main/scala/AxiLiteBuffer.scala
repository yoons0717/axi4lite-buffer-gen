import chisel3._

class AxiLiteBuffer(c: AxiLiteConfig) extends Module {
  // AxiLiteIO는 master 기준 → 슬레이브는 전체를 한 번 더 뒤집음 (aw/w/ar 입력, b/r 출력)
  val io = IO(Flipped(new AxiLiteIO(c)))

  // 레지스터 뱅크: numRegs칸, 칸마다 dataWidth비트
  val regs = RegInit(VecInit(Seq.fill(c.numRegs)(0.U(c.dataWidth.W))))
}
