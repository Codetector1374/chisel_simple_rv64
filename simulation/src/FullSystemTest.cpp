//
// Created by user on 11/28/2021.
//

#include "testbench.h"
#include "VFullSystemTop.h"
#include <stdlib.h>

std::vector<uint32_t> read_file(const std::string &file) {
    std::vector<uint32_t> words;
    FILE *f = fopen(file.c_str(), "r");
    if (!f) {
        printf("Can not open file %s", file.c_str());
        exit(1);
    }
    unsigned char temp[4];
    int read = 0;
    while (true) {

        int r = fread(temp, 1, 4 - read, f);
        if (r == 0) {
            break;
        }

        read += r;

        if (read == 4) {
            words.push_back(*(unsigned int*)temp);
            read = 0;
        }
    }
    printf("Initialized memory with %zu words\n", words.size());
    fclose(f);
    return words;
}

int main(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: %s <ram_init>\n", argv[0]);
    }
    auto *tb = new TESTBENCH<VFullSystemTop>();
    auto &mem = tb->m_core->FullSystemTop__DOT__ramWB_memdev__DOT__mem;

    auto content = read_file(argv[1]);
    for(size_t i = 0; i < content.size(); i++) {
        mem[i] = content[i];
    }

    tb->opentrace("test_trace.vcd");

    tb->reset();

    for (int i = 0; i < 30000; ++i) {
        tb->tick();
//        if (tb->m_core->io_leds == 0x6969) {
//            printf("Success\n");
//            exit(0);
//        } else if (tb->m_core->io_leds != 0) {
//            printf("Fail: leds = %04x\n", tb->m_core->io_leds);
//            exit(1);
//        }
    }
    printf("Didn't finish\n");

//    tb->close();

}
