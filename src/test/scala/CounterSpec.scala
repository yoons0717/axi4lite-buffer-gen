import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

/** Counter 스모크 테스트.
  * 참고: docs/d05-chisel-basics-and-counter.md §4 (EphemeralSimulator: poke/step/expect, reset 필수)
  */
class CounterSpec extends AnyFlatSpec {

  // EphemeralSimulator는 자동 리셋을 안 해준다 — 직접 pulse해야 RegInit 값이 적용된다.
  def resetDut(c: Counter): Unit = {
    c.reset.poke(true.B)
    c.clock.step(1)
    c.reset.poke(false.B)
  }

  "Counter" should "count up by 1 each cycle while en is high" in {
    simulate(new Counter) { c =>
      resetDut(c)

      // TODO 1: io.en 을 true.B 로 poke
      c.io.en.poke(true.B)
      // TODO 2: clock.step(10) 으로 10사이클 진행
      c.clock.step(10)
      // TODO 3: io.count 가 10.U 인지 expect
      c.io.count.expect(10.U)
    }
  }

  it should "wrap around after 255 (8-bit overflow)" in {
    simulate(new Counter) { c =>
      resetDut(c)

      // TODO 4: io.en 을 true.B 로 poke
      c.io.en.poke(true.B)
      // TODO 5: 260 사이클 진행 (260 mod 256 = 4)
      c.clock.step(260)
      c.io.count.expect(4.U)
    }
  }
}
