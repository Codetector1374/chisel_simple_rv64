#include "system.h"
#include "hardware.h"

void wait_ms(uint32_t ms) {
    TIMER1 = 0;
    while(TIMER1 < ms){}
}

void _putchar(char c) {
    if(c == '\n') {
        UART_DATA = '\r';
    }
    UART_DATA = c;
}