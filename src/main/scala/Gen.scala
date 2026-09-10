import circt.stage.ChiselStage

/** SystemVerilog 생성 진입점.
  *  실행: sbt "runMain Gen"
  *  결과: generated/Counter.sv
  */
object Gen extends App {
  ChiselStage.emitSystemVerilogFile(
    new Counter,
    Array("--target-dir", "generated")
  )
}
