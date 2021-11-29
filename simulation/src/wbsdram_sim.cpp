//
// Created by user on 11/25/2021.
//

#include "testbench.h"
#include "VTestWBMaster.h"
#include "sdramsim.h"

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);
    auto *tb = new TESTBENCH<VTestWBMaster>();
    VTestWBMaster *t = tb->m_core;
    SDRAMSIM *sdram = new SDRAMSIM();

    bool trace = false;

//    tb->opentrace("sdram_trace.vcd");
    tb->reset();
    tb->tick();
    unsigned char data[] = {0x01, 0x02, 0x03, 0x04};
    sdram->load(0, reinterpret_cast<const char *>(data), 4);

    t->io_switches = 0x26;
    t->io_write = 1;

    int not_stall = 0;
    for (int i = 0; i < 40000000; ++i) {
        if ((!trace) && sdram->pwrup()) {
            trace = true;
            tb->opentrace("sdram_trace.vcd");
        }
        if(!t->TestWBMaster__DOT__ramController_io_wb_stall) {
            not_stall++;
            t->io_write = !t->io_write;
        }
        t->io_dqIn = (*sdram)(t->io_sdram_clk, t->io_sdram_cke,
                t->io_sdram_cs_n, t->io_sdram_ras_n, t->io_sdram_cas_n, t->io_sdram_we_n, t->io_sdram_bank,
                t->io_sdram_addr,
                t->io_sdram_output_en, t->io_sdram_dataOut, t->io_sdram_dqm);
        tb->downtick();
        tb->uptick();
        if (i % 50000 == 0) {
            printf("current cycle is %lu, which at 50Mhz is %lu ms (not stall %d)\n", tb->m_tickcount, tb->m_tickcount / 50000, not_stall);
        }
    }

    tb->close();

    printf("END: current cycle is %lu, which at 50Mhz is %lu us\n", tb->m_tickcount, tb->m_tickcount / 50);
}