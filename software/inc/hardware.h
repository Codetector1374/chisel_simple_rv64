#pragma once

#include <stdint.h>
#include <stddef.h>
#include <mii_net.h>

#define LEDS        (*((volatile uint32_t*) 0xF0000000))
#define HEX         (*((volatile uint32_t*) 0xF0000004))
#define TIMER1      (*((volatile uint32_t*) 0xF0000010))
#define UART_CMD    (*((volatile uint32_t*) 0xF0001000))
#define UART_DATA   (*((volatile uint32_t*) 0xF0001004))
#define NEOPIXELS   ((volatile uint32_t*) 0xF0002000)

#define ETHER0      (*((MIIEthernet_t *) 0xF1000000))