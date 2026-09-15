import chisel3._
import chisel3.util._ 


class ParametricFIFO(dataWidth: Int, depth: Int) extends Module {
    val io = IO(new Bundle {
        val dataIn = Input(UInt(dataWidth.W))
        val dataOut = Output(UInt(dataWidth.W))
        val push  = Input(Bool())
        val pop   = Input(Bool())
        val full  = Output(Bool())
        val empty = Output(Bool())
    })

    val mem = Mem(depth, UInt(dataWidth.W))
    val writePtr = RegInit(0.U(log2Ceil(depth).W))
    val readPtr = RegInit(0.U(log2Ceil(depth).W))
    val count = RegInit(0.U(log2Ceil(depth + 1).W))

    val doPush = io.push && !io.full
    val doPop  = io.pop && !io.empty


    when(doPush) {
        mem(writePtr) := io.dataIn
        writePtr := writePtr + 1.U
    }

    when(doPop) {
        readPtr := readPtr + 1.U
    }

    when(doPush && !doPop) {
        count := count + 1.U
    } .elsewhen(doPop && !doPush) {
        count := count - 1.U
    }
   
    io.dataOut := mem(readPtr)

    io.full := (count === depth.U)
    io.empty := (count === 0.U)

    require(isPow2(depth), "depth must be power of 2")

}
