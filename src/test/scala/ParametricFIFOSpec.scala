import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

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
}