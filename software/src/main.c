#include <stdint.h>
#include <stddef.h>
#include "hardware.h"
#include "system.h"

_Noreturn void cmain(void) {
    UART_CMD = 0;

    LEDS = 0;
    eth_init(&ETHER0);
    eth_wait_link(&ETHER0);

    uint8_t frame[] = {
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 
        0x02, 0x12, 0x34, 0x56, 0x78, 0x90,
        0x08, 0x06
    };

    for(;;) {
        for (size_t i = 0; i < sizeof(frame); i++)
        {
            ETHER0.pushByte = frame[i];
        }
        HEX = ETHER0.txCommandStatus;
        ETHER0.txCommandStatus = 0;
        while((HEX = ETHER0.txCommandStatus)){}
        wait_ms(1000);
    }
    

    for(;;) {}
}