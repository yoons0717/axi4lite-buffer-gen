import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class SimpleFSMSpec extends AnyFlatSpec {

  // EphemeralSimulator는 자동 리셋을 안 해준다 — 직접 pulse해야 RegInit 값이 적용된다.
  def resetDut(c: SimpleFSM): Unit = {
    c.reset.poke(true.B)
    c.clock.step(1)
    c.reset.poke(false.B)
  }

  "SimpleFSM" should "rw=0이면 Read로 1사이클 갔다가 Idle로 돌아온다" in {
    simulate(new SimpleFSM) { c =>
      resetDut(c)

      c.io.start.poke(true.B)
      c.io.rw.poke(false.B)
      c.clock.step(1)

      c.io.state.expect(St.Read)
      c.io.done.expect(true.B)
      c.clock.step(1)
      c.io.state.expect(St.Idle)
      c.io.done.expect(false.B)
    }
  }

  it should "rw=1이면 Write로 1사이클 갔다가 Idle로 돌아온다" in {
    simulate(new SimpleFSM) { c =>
      resetDut(c)

      c.io.start.poke(true.B)
      c.io.rw.poke(true.B)
      c.clock.step(1)

      c.io.state.expect(St.Write)
      c.io.done.expect(true.B)
      c.clock.step(1)
      c.io.state.expect(St.Idle)
      c.io.done.expect(false.B)
    }
  }
}
