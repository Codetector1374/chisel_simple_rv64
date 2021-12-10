//
// Created by user on 12/8/2021.
//

#include "Vcrc32.h"
#include "verilated_vcd_c.h"

VerilatedVcdC *vcd;
Vcrc32 *dut;
void tick() {
    static int counter = 1;
    vcd->dump(2*counter - 1);
    vcd->flush();

    dut->eval();
    dut->clk = 1;
    dut->eval();
    vcd->dump(2*counter);
    vcd->flush();

    dut->clk = 0;
    dut->eval();
    counter++;
}

unsigned char reverse(unsigned char b) {
    b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
    b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
    b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
    return b;
}

int main() {
    uint8_t packet[] = {0xff,0xff,0xff,0xff,0xff,0xff,0x18,0xc0,0x4d,0x09,0x1e,0x50,0x08,0x06,0x00,0x01,0x08,0x00,0x06,0x04,0x00,0x01,0x18,0xc0,0x4d,0x09,0x1e,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x0a,0x63,0x63,0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
//    uint8_t  packet[] = {0x12 ,0x34 ,0x56 ,0x78};

    Verilated::traceEverOn(true);
    dut = new Vcrc32;
    vcd = new VerilatedVcdC;
    dut->trace(vcd, 10);
    vcd->open("crc32.vcd");

    dut->rst = 1;
    tick();
    dut->rst = 0;
    tick();

    for (int i = 0; i < 1000; ++i) {
        if (i < 2 * sizeof(packet)) {
            unsigned char thing = reverse(packet[i/2]);
            if (i % 2 == 0) {
                dut->data_in = (thing >> 4) & 0xF;
            } else {
                dut->data_in = (thing) & 0xF;
            }
            dut->crc_en = 1;
        } else {
            dut->crc_en = 0;
        }
        tick();
    }


}