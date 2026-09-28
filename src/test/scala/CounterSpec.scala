import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

/** Counter 스모크 테스트. */
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

      c.io.en.poke(true.B)
      c.clock.step(10)
      c.io.count.expect(10.U)
    }
  }

  it should "wrap around after 255 (8-bit overflow)" in {
    simulate(new Counter) { c =>
      resetDut(c)

      c.io.en.poke(true.B)
      c.clock.step(260)
      c.io.count.expect(4.U)
    }
  }
}
