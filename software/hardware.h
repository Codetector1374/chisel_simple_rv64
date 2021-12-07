#include <stdint.h>
#include <stddef.h>


#pragma pack(push, 1)
typedef struct {
    volatile uint8_t mgmtRegAddr;
    volatile uint8_t mgmtPhyAddr;
    uint16_t _pad1;

    volatile uint32_t mgmtPort;

} MIIEthernet_t;
#pragma pack(pop)

// Register 0
#define MII_CTRL_RESET      (1U << 15U)
#define MII_CTRL_100MBPS    (0b1U << 13U)
#define MII_CTRL_FULL_DUP   (0b1U << 8U)

// Regiser 17
#define MII_SPECSTS_LINK_UP     (0b1U << 10U)


#define LEDS        (*((volatile uint32_t*) 0xF0000000))
#define HEX         (*((volatile uint32_t*) 0xF0000004))
#define TIMER1      (*((volatile uint32_t*) 0xF0000010))
#define UART_CMD    (*((volatile uint32_t*) 0xF0001000))
#define UART_DATA   (*((volatile uint32_t*) 0xF0001004))
#define NEOPIXELS   ((volatile uint32_t*) 0xF0002000)
#define ETHER0      (*((MIIEthernet_t *) 0xF1000000))