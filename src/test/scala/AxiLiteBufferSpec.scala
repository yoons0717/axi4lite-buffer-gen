import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable
import scala.util.Random

class AxiLiteBufferSpec extends AnyFlatSpec {

  val OKAY: BigInt   = 0
  val SLVERR: BigInt = 2

  def resetDut(c: AxiLiteBuffer): Unit = {
    c.reset.poke(true.B)
    c.clock.step(1)
    c.reset.poke(false.B)
  }

  // AW+W를 동시에 valid로 걸고, 각자 fire(=ready가 뜬 사이클)할 때까지 기다린 뒤
  // B 응답을 받는다. 응답 코드(BigInt)를 돌려준다.
  def doWrite(c: AxiLiteBuffer, addr: BigInt, data: BigInt, strb: BigInt): BigInt = {
    c.io.aw.valid.poke(true.B)
    c.io.aw.bits.addr.poke(addr)
    c.io.aw.bits.prot.poke(0)
    c.io.w.valid.poke(true.B)
    c.io.w.bits.data.poke(data)
    c.io.w.bits.strb.poke(strb)

    var awFired = false
    var wFired  = false
    while (!awFired || !wFired) {
      if (!awFired && c.io.aw.ready.peekBoolean()) awFired = true
      if (!wFired && c.io.w.ready.peekBoolean()) wFired = true
      c.clock.step(1)
      if (awFired) c.io.aw.valid.poke(false.B)
      if (wFired) c.io.w.valid.poke(false.B)
    }

    c.io.b.ready.poke(true.B)
    while (!c.io.b.valid.peekBoolean()) c.clock.step(1)
    val resp = c.io.b.bits.resp.peek().litValue
    c.clock.step(1)
    c.io.b.ready.poke(false.B)
    resp
  }

  // AR을 걸고 fire할 때까지 기다린 뒤 R 응답(데이터, 응답코드)을 받는다.
  def doRead(c: AxiLiteBuffer, addr: BigInt): (BigInt, BigInt) = {
    c.io.ar.valid.poke(true.B)
    c.io.ar.bits.addr.poke(addr)
    c.io.ar.bits.prot.poke(0)
    while (!c.io.ar.ready.peekBoolean()) c.clock.step(1)
    c.clock.step(1)
    c.io.ar.valid.poke(false.B)

    c.io.r.ready.poke(true.B)
    while (!c.io.r.valid.peekBoolean()) c.clock.step(1)
    val data = c.io.r.bits.data.peek().litValue
    val resp = c.io.r.bits.resp.peek().litValue
    c.clock.step(1)
    c.io.r.ready.poke(false.B)
    (data, resp)
  }

  "AxiLiteBuffer" should "쓴 값을 그대로 읽어온다 (directed write→read)" in {
    simulate(new AxiLiteBuffer(AxiLiteConfig(32, 32, 16))) { c =>
      resetDut(c)
      val wResp = doWrite(c, addr = 0x08, data = BigInt("DEADBEEF", 16), strb = 0xF)
      assert(wResp == OKAY)
      val (data, rResp) = doRead(c, addr = 0x08)
      assert(rResp == OKAY)
      assert(data == BigInt("DEADBEEF", 16))
    }
  }

  it should "WSTRB로 일부 바이트만 갱신한다 (partial write)" in {
    simulate(new AxiLiteBuffer(AxiLiteConfig(32, 32, 16))) { c =>
      resetDut(c)
      doWrite(c, addr = 0x00, data = BigInt("11223344", 16), strb = 0xF)
      // strb=0b1001 → byte0·byte3만 갱신, byte1(22)·byte2(33)는 기존 값 유지
      doWrite(c, addr = 0x00, data = BigInt("AABBCCDD", 16), strb = 0x9)
      val (data, resp) = doRead(c, addr = 0x00)
      assert(resp == OKAY)
      assert(data == BigInt("AA2233DD", 16))
    }
  }

  it should "AW/W가 서로 다른 사이클에 도착해도(핸드셰이크 스톨) 정확히 write한다" in {
    simulate(new AxiLiteBuffer(AxiLiteConfig(32, 32, 16))) { c =>
      resetDut(c)

      // AW만 먼저 걸어서 fire시킨다.
      c.io.aw.valid.poke(true.B)
      c.io.aw.bits.addr.poke(0x04)
      c.io.aw.bits.prot.poke(0)
      while (!c.io.aw.ready.peekBoolean()) c.clock.step(1)
      c.clock.step(1)
      c.io.aw.valid.poke(false.B)

      // W는 몇 사이클 뒤에 도착 — 그동안 슬레이브는 awDone=true로 W만 기다린다.
      c.clock.step(3)

      c.io.w.valid.poke(true.B)
      c.io.w.bits.data.poke(0x77)
      c.io.w.bits.strb.poke(0xF)
      while (!c.io.w.ready.peekBoolean()) c.clock.step(1)
      c.clock.step(1)
      c.io.w.valid.poke(false.B)

      c.io.b.ready.poke(true.B)
      while (!c.io.b.valid.peekBoolean()) c.clock.step(1)
      val resp = c.io.b.bits.resp.peek().litValue
      c.clock.step(1)
      c.io.b.ready.poke(false.B)
      assert(resp == OKAY)

      val (data, _) = doRead(c, addr = 0x04)
      assert(data == 0x77)
    }
  }

  it should "범위 밖 주소는 SLVERR을 응답한다" in {
    simulate(new AxiLiteBuffer(AxiLiteConfig(32, 32, 16))) { c =>
      resetDut(c)
      // numRegs=16, strbWidth=4 → addrSpan=64(0x40). 0x40은 범위 밖.
      val wResp = doWrite(c, addr = 0x40, data = 1, strb = 0xF)
      assert(wResp == SLVERR)

      val (rData, rResp) = doRead(c, addr = 0x40)
      assert(rResp == SLVERR)
      assert(rData == 0)
    }
  }

  it should "랜덤 트랜잭션을 참조 모델(scoreboard)과 대조한다" in {
    val cfg = AxiLiteConfig(32, 32, 16)
    simulate(new AxiLiteBuffer(cfg)) { c =>
      resetDut(c)

      val model = mutable.Map[BigInt, BigInt]().withDefaultValue(BigInt(0))
      val rnd   = new Random(2)

      for (_ <- 0 until 150) {
        val idx  = rnd.nextInt(cfg.numRegs)
        val addr = BigInt(idx * cfg.strbWidth)

        if (rnd.nextBoolean()) {
          val data = BigInt(cfg.dataWidth, rnd.self)
          val strb = BigInt(cfg.strbWidth, rnd.self)
          val resp = doWrite(c, addr, data, strb)
          assert(resp == OKAY)

          var next = model(addr)
          for (i <- 0 until cfg.strbWidth) {
            if (strb.testBit(i)) {
              val byteMask = BigInt(0xFF) << (8 * i)
              next = (next & ~byteMask) | (data & byteMask)
            }
          }
          model(addr) = next
        } else {
          val (data, resp) = doRead(c, addr)
          assert(resp == OKAY)
          assert(data == model(addr))
        }
      }
    }
  }
}
