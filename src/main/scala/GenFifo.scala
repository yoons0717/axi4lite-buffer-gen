import circt.stage.ChiselStage

/** ParametricFIFO를 두 config로 각각 생성.
  *  실행: sbt "runMain GenFifo"
  *  결과: generated/fifo_w32_d16/ParametricFIFO.sv, generated/fifo_w64_d64/ParametricFIFO.sv
  */
object GenFifo extends App {
  ChiselStage.emitSystemVerilogFile(
    new ParametricFIFO(32, 16),
    Array("--target-dir", "generated/fifo_w32_d16")
  )
  ChiselStage.emitSystemVerilogFile(
    new ParametricFIFO(64, 64),
    Array("--target-dir", "generated/fifo_w64_d64")
  )
}
