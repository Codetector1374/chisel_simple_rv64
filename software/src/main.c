#include <stdint.h>
#include <stddef.h>
#include "hardware.h"
#include "system.h"
#include "printf.h"

#include "net/ethernet.h"

_Noreturn void cmain(void) {
    UART_CMD = 0;

    LEDS = 0;
    eth_init(&ETHER0);
    eth_wait_link(&ETHER0);

    uint8_t inspectionBuffer[1024];

    for(;;) {
        if (ETHER0.rxHasFrame) {
            uint16_t size = ETHER0.rxFrameSize;
            printf("Got a frame of size %d\n", size);
            ETHER0.rxFrameAddress = 0;

            for (int i = 0; i < size; ++i) {
                uint8_t  byte = ETHER0.rxGetByte;
                if (i % 16 == 0) {
                    printf("\n");
                }
                printf("%02X ", byte);
            }
            ETHER0.rxClearFrame = 0;
            printf("\nEND\n");
        }
    }
    

    for(;;) {}
}