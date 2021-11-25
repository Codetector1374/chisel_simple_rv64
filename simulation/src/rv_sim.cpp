#include "stdio.h"
#include "verilated_vcd_c.h"
#include "VTop.h"
#include "testbench.h"

int main(int argc, char** argv) {
    Verilated::commandArgs(argc, argv);
    auto *tb = new TESTBENCH<VTop>();
    VTop *top = tb->m_core;

    auto &mem = top->Top__DOT__memdev__DOT__mem;
    mem[0] = 0x12300093;
    mem[1] = 0x00108133;
    mem[2] = 0x02102023;
    mem[3] = 0x02102183;
    mem[4] = 0xff5ff06f;
    mem[5] = 0x0ff00093;

    tb->opentrace("trace.vcd");
    tb->reset();
    tb->tick();

    for (int i = 0; i < 100; ++i) {
        tb->tick();
    }

    tb->close();

    printf("mem[0x20] = %08x\n", mem[0x20 / 4]);

    printf("hello world\n");
}