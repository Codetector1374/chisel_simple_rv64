#pragma once

#include <stdint.h>
#include <stddef.h>

#pragma pack(push, 1)
typedef struct {
  volatile uint8_t mgmtRegAddr;
  volatile uint8_t mgmtPhyAddr;
  uint16_t _pad1;

  volatile uint32_t mgmtPort;

  volatile uint8_t pushByte;
  uint8_t _pad2;
  uint16_t _pad3;

  volatile uint32_t txCommandStatus;

  union {
    struct {
      volatile uint8_t rxHasFrame;
      uint8_t _pad;
      uint16_t rxFrameSize;
    };
    volatile uint32_t rxClearFrame;
  };

  union {
    volatile uint32_t   rxFrameAddress;
    volatile uint8_t    rxGetByte;
  };

} MIIEthernet_t;
#pragma pack(pop)

// Register 0
#define MII_CTRL_RESET      (1U << 15U)
#define MII_CTRL_100MBPS    (0b1U << 13U)
#define MII_CTRL_FULL_DUP   (0b1U << 8U)

// Regiser 17
#define MII_SPECSTS_LINK_UP     (0b1U << 10U)

void eth_init(MIIEthernet_t *eth);
void eth_wait_link(MIIEthernet_t *eth);
void eth_send_frame(MIIEthernet_t *eth, const void* frame, size_t size);
/**
 * check if MIIEthernet eth has a frame
 * @param eth
 * @return size of the pending frame
 */
uint16_t eth_has_frame(MIIEthernet_t *eth);

