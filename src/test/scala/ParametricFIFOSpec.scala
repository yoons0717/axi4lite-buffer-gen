import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable
import scala.util.Random

class ParametricFIFOSpec extends AnyFlatSpec {

  def resetDut(c: ParametricFIFO): Unit = {
    c.reset.poke(true.B)
    c.clock.step(1)
    c.reset.poke(false.B)
  }

  "ParametricFIFO" should "채우면 full이 된다" in {
    simulate(new ParametricFIFO(8, 4)) { c =>
        resetDut(c)

        for (i <- 0 until 4) {
            c.io.push.poke(true.B)
            c.clock.step(1)
        }
        c.io.full.expect(true.B)

    }
  }

  it should "비우면 넣은 순서대로 나오고 empty가 된다" in {
    simulate(new ParametricFIFO(8, 4)) { c =>
      resetDut(c)

      val values = Seq(10, 20, 30, 40)

      for (v <- values) {
        c.io.dataIn.poke(v.U)
        c.io.push.poke(true.B)
        c.clock.step(1)
      }
      c.io.push.poke(false.B)

      for (v <- values) {
        c.io.dataOut.expect(v.U)
        c.io.pop.poke(true.B)
        c.clock.step(1)
      }
      c.io.empty.expect(true.B)
    }
  }

  it should "랜덤 push/pop을 참조 모델(mutable.Queue)과 대조한다 (STEP 14)" in {
    simulate(new ParametricFIFO(8, 4)) { c =>
      resetDut(c)

      val model = mutable.Queue[BigInt]()
      val rnd   = new Random(1)

      for (_ <- 0 until 200) {
        // 이번 사이클의 시도 — full/empty와 무관하게 무작위로 걸어서
        // DUT가 실제로 가드를 지키는지(꽉 찼을 때 push 무시, 비었을 때 pop 무시)까지 확인한다.
        val tryPush = rnd.nextBoolean()
        val tryPop  = rnd.nextBoolean()

        // 이번 엣지 이전(=현재 레지스터 기준) 상태로 판단 — DUT의 doPush/doPop과 같은 기준.
        val wasFull  = model.size == c.depth
        val wasEmpty = model.isEmpty
        c.io.full.expect(wasFull.B)
        c.io.empty.expect(wasEmpty.B)

        val expectPush = tryPush && !wasFull
        val expectPop  = tryPop && !wasEmpty

        // Mem은 비동기 read라서, step 하기 전에 이미 head 값이 나와있다.
        if (expectPop) c.io.dataOut.expect(model.front)

        val v = BigInt(rnd.nextInt(256))
        c.io.dataIn.poke(v.U)
        c.io.push.poke(tryPush.B)
        c.io.pop.poke(tryPop.B)
        c.clock.step(1)

        if (expectPush) model.enqueue(v)
        if (expectPop) model.dequeue()
      }

      // 루프가 끝나도 model에 아직 안 빠진 값이 남아있을 수 있다 — 그 값들은 지금까지
      // 한 번도 dataOut으로 확인된 적이 없다. 마저 비우면서 끝까지 검증한다.
      c.io.push.poke(false.B)
      while (model.nonEmpty) {
        c.io.dataOut.expect(model.front)
        c.io.pop.poke(true.B)
        c.clock.step(1)
        model.dequeue()
      }
      c.io.empty.expect(true.B)
    }
  }
}