#pragma once

#include <stdint.h>

#pragma pack(push, 1)

typedef struct {
  uint8_t addr[6];
} mac_address_t;

typedef uint16_t ether_type_t;

typedef struct {
  mac_address_t dest;
  mac_address_t src;
  ether_type_t type;
} ethernet_header_t;
#pragma pack(pop)

#define ETHER_TYPE_IPv4     ((ether_type_t) 0x0800)
#define ETHER_TYPE_ARP      ((ether_type_t) 0x0806)
