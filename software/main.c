#include <stdint.h>
#include <stddef.h>

int rand();
int rseed = 0;
 
inline void srand(int x)
{
	rseed = x;
}

#define RAND_MAX_32 ((1U << 31) - 1)

inline int rand()
{
	return (rseed = (rseed * 214013 + 2531011) & RAND_MAX_32) >> 16;
}

uint32_t data;
#define LEDS (*((volatile uint32_t*) 0xF0000000))
#define HEX (*((volatile uint32_t*) 0xF0000004))
#define UART_CMD (*((volatile uint32_t*) 0xF0001000))
#define UART_DATA (*((volatile uint32_t*) 0xF0001004))

#define RAM ((volatile uint32_t*)0xE0000000)
// #define RAM ((volatile uint32_t*)0x1000)


int memtest_addr_pattern(volatile uint32_t *start, size_t size) {
    size_t x = size / 4;
    for(size_t i = size; i > 0; i--) {
        start[i-1] = i-1;
    }
    for(size_t i = 0; i < x; i++) {
        if (start[i] != i) {
            HEX = i;
            for(;;){
                LEDS = start[i];
            }
            return 0;
        }
    }
    return 1;
}

void cmain(void) {
    volatile uint32_t y = 0;
    UART_CMD = 0;
    for(;;) {
        if (UART_CMD & 0xFFFF) {
            uint32_t data = UART_DATA;
            HEX = data;
            UART_DATA = data;
        }

        // LEDS = LEDS;
        // HEX = LEDS;
    }
    
}