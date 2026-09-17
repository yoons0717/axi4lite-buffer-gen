import circt.stage.ChiselStage

/** AxiLiteBuffer를 두 config로 각각 생성.
  *  실행: sbt "runMain GenAxiLiteBuffer"
  *  결과: generated/axi_w32_n16/AxiLiteBuffer.sv, generated/axi_w64_n64/AxiLiteBuffer.sv
  */
object GenAxiLiteBuffer extends App {
  ChiselStage.emitSystemVerilogFile(
    new AxiLiteBuffer(AxiLiteConfig(32, 32, 16)),
    Array("--target-dir", "generated/axi_w32_n16")
  )
  ChiselStage.emitSystemVerilogFile(
    new AxiLiteBuffer(AxiLiteConfig(32, 64, 64)),
    Array("--target-dir", "generated/axi_w64_n64")
  )
}
