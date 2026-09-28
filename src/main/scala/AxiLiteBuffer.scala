import chisel3._
import chisel3.util._

object WState extends ChiselEnum { val W_IDLE, W_DATA, W_RESP = Value }
object RState extends ChiselEnum { val R_IDLE, R_RESP = Value }

class AxiLiteBuffer(c: AxiLiteConfig) extends Module {
  // AxiLiteIO는 master 기준 → 슬레이브는 전체를 한 번 더 뒤집음 (aw/w/ar 입력, b/r 출력)
  val io = IO(Flipped(new AxiLiteIO(c)))

  // 레지스터 뱅크: numRegs칸, 칸마다 dataWidth비트
  val regs = RegInit(VecInit(Seq.fill(c.numRegs)(0.U(c.dataWidth.W))))

  def decode(addr: UInt): (UInt, Bool) = {
    val index   = addr(c.byteOffset + c.regIndexBits - 1, c.byteOffset)
    val inRange = addr < c.addrSpan.U
    (index, inRange)
  }

  val wState = RegInit(WState.W_IDLE) // 현재 단계
  val awDone = RegInit(false.B) // aw 채널에서 valid를 받았는지 여부
  val wDone  = RegInit(false.B) // w 채널에서 valid를 받았는지 여부
  val awAddr = Reg(UInt(c.addrWidth.W)) // aw 채널에서 받은 주소를 저장하는 레지스터
  val wData  = Reg(UInt(c.dataWidth.W)) // w 채널에서 받은 데이터를 저장하는 레지스터
  val wStrb  = Reg(UInt(c.strbWidth.W)) // w 채널에서 받은 스트로브를 저장하는 레지스터

  // ready/valid는 상태로만 결정 (조합 루프 방지)
  io.aw.ready := (wState === WState.W_IDLE) && !awDone
  io.w.ready  := (wState === WState.W_IDLE) && !wDone
  io.b.valid  := (wState === WState.W_RESP)

  when (io.aw.fire) { awAddr := io.aw.bits.addr; awDone := true.B }
  when (io.w.fire)  { wData := io.w.bits.data; wStrb := io.w.bits.strb; wDone := true.B }

  val (index, inRange) = decode(awAddr)
  io.b.bits.resp := Mux(inRange, 0.U, 2.U)  // OKAY(0) / SLVERR(2)

  switch (wState) {
    is (WState.W_IDLE) {
      when (awDone && wDone) { wState := WState.W_DATA }  // 둘 다 받으면 진행
    }
    is (WState.W_DATA) {
      // WSTRB 마스킹: strb(i)=1이면 새 바이트, 0이면 기존 바이트 유지
      val cur  = regs(index)
      val next = Cat((0 until c.strbWidth).reverse.map { i =>
        Mux(wStrb(i), wData(8 * i + 7, 8 * i), cur(8 * i + 7, 8 * i))
      })
      when (inRange) { regs(index) := next }
      wState := WState.W_RESP
    }
    is (WState.W_RESP) {
      when (io.b.fire) {  // 응답 받아갔으면 다음 트랜잭션 준비
        wState := WState.W_IDLE
        awDone := false.B
        wDone  := false.B
      }
    }
  }

  // ---- read 경로 ----
  // write와 독립된 별도 상태 레지스터로 병렬로 돈다. 레지스터 read가 조합이라
  // W_DATA 같은 중간 단계 없이 AR 받은 다음 바로 응답 단계로 간다.
  val rState = RegInit(RState.R_IDLE)
  val arAddr = Reg(UInt(c.addrWidth.W))  // AR fire 시점 주소 래치

  io.ar.ready := (rState === RState.R_IDLE)   // 상태로만 결정 (조합 루프 방지)
  io.r.valid  := (rState === RState.R_RESP)

  when (io.ar.fire) { arAddr := io.ar.bits.addr }

  val (rIndex, rInRange) = decode(arAddr)
  io.r.bits.data := Mux(rInRange, regs(rIndex), 0.U)  // 범위밖이면 0
  io.r.bits.resp := Mux(rInRange, 0.U, 2.U)            // OKAY(0) / SLVERR(2)

  switch (rState) {
    is (RState.R_IDLE) {
      when (io.ar.fire) { rState := RState.R_RESP }  // AR 하나만 받으면 바로 진행
    }
    is (RState.R_RESP) {
      when (io.r.fire) { rState := RState.R_IDLE }  // 응답 받아갔으면 복귀
    }
  }
}
