import chisel3._
import chisel3.util.{log2Ceil, Decoupled}

case class AxiLiteConfig(addrWidth: Int = 32, dataWidth: Int = 32, numRegs: Int = 16) {
  require(dataWidth == 32 || dataWidth == 64, "AXI4-Lite data width must be 32 or 64")
  require(numRegs > 0)

  val strbWidth    = dataWidth / 8                       // WSTRB 비트 수
  val byteOffset   = log2Ceil(strbWidth)                 // 워드 내 오프셋 비트 (32→2, 64→3)
  val regIndexBits = log2Ceil(numRegs)                   // 레지스터 인덱스 비트
  val addrSpan     = numRegs * strbWidth                 // 매핑된 바이트 범위 [0, addrSpan)
}

class AWPayload(c: AxiLiteConfig) extends Bundle {
  val addr = UInt(c.addrWidth.W)
  val prot = UInt(3.W)
}

class WPayload(c: AxiLiteConfig) extends Bundle {
  val data = UInt(c.dataWidth.W)
  val strb = UInt(c.strbWidth.W)
}

class BPayload extends Bundle {
  val resp = UInt(2.W)
}

class ARPayload(c: AxiLiteConfig) extends Bundle {
  val addr = UInt(c.addrWidth.W)
  val prot = UInt(3.W)
}

class RPayload(c: AxiLiteConfig) extends Bundle {
  val data = UInt(c.dataWidth.W)
  val resp = UInt(2.W)
}

class AxiLiteIO(c: AxiLiteConfig) extends Bundle {  // master 관점: valid를 내가 낼 때는 그대로, slave가 낼 땐 Flipped
  val aw = Decoupled(new AWPayload(c))
  val w  = Decoupled(new WPayload(c))
  val b  = Flipped(Decoupled(new BPayload))    // slave가 valid를 몬다
  val ar = Decoupled(new ARPayload(c))
  val r  = Flipped(Decoupled(new RPayload(c))) // slave가 valid를 몬다
}