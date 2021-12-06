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
#define TIMER1 (*((volatile uint32_t*) 0xF0000010))
#define UART_CMD (*((volatile uint32_t*) 0xF0001000))
#define UART_DATA (*((volatile uint32_t*) 0xF0001004))
#define NEOPIXELS ((volatile uint32_t*) 0xF0002000)

#define RAM ((volatile uint32_t*)0xE0000000)
// #define RAM ((volatile uint32_t*)0x1000)

int mypow(int x, int y) {
    int res = 1;
    for (int i = 0; i < y; i++)
    {
        res *= x;
    }
    return res;
}

typedef struct RgbColor
{
    unsigned char r;
    unsigned char g;
    unsigned char b;
} RgbColor;

typedef struct HsvColor
{
    unsigned char h;
    unsigned char s;
    unsigned char v;
} HsvColor;

RgbColor HsvToRgb(HsvColor hsv)
{
    RgbColor rgb;
    unsigned char region, remainder, p, q, t;

    if (hsv.s == 0)
    {
        rgb.r = hsv.v;
        rgb.g = hsv.v;
        rgb.b = hsv.v;
        return rgb;
    }

    region = hsv.h / 43;
    remainder = (hsv.h - (region * 43)) * 6; 

    p = (hsv.v * (255 - hsv.s)) >> 8;
    q = (hsv.v * (255 - ((hsv.s * remainder) >> 8))) >> 8;
    t = (hsv.v * (255 - ((hsv.s * (255 - remainder)) >> 8))) >> 8;

    switch (region)
    {
        case 0:
            rgb.r = hsv.v; rgb.g = t; rgb.b = p;
            break;
        case 1:
            rgb.r = q; rgb.g = hsv.v; rgb.b = p;
            break;
        case 2:
            rgb.r = p; rgb.g = hsv.v; rgb.b = t;
            break;
        case 3:
            rgb.r = p; rgb.g = q; rgb.b = hsv.v;
            break;
        case 4:
            rgb.r = t; rgb.g = p; rgb.b = hsv.v;
            break;
        default:
            rgb.r = hsv.v; rgb.g = p; rgb.b = q;
            break;
    }

    return rgb;
}


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

void doLEDs(void) {
    HsvColor sourceColor = {0, 255, 10};
    RgbColor destColor;
    for(;;) {
        sourceColor.v = LEDS & 0xFF;
        sourceColor.h ++;
        destColor = HsvToRgb(sourceColor);
        uint32_t color = ((uint32_t)destColor.g) << 16 | ((uint32_t)destColor.r) << 8 | ((uint32_t)destColor.b);
        for (int i = 0; i < 10; i++) {
            NEOPIXELS[i] = color;
        }
        TIMER1 = 0;
        while(TIMER1 < 10){}
    }
}

void cmain(void) {
    volatile uint32_t y = 0;
    // UART_CMD = 0;
    int x = 0;
    for(;;) {
        HEX = TIMER1 / 1000;
    }
}