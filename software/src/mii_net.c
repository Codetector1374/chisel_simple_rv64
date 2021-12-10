//
// Created by user on 12/9/2021.
//
#include "mii_net.h"

void eth_init(MIIEthernet_t *eth) {
    eth->mgmtPhyAddr = 0b10000;
    eth->mgmtRegAddr = 0;
    uint16_t currentControl;
    do {
        currentControl = eth->mgmtPort;
    } while(currentControl & MII_CTRL_RESET);
    uint16_t newContorl = MII_CTRL_RESET | MII_CTRL_100MBPS | MII_CTRL_FULL_DUP | (currentControl&0x3F);
    eth->mgmtPort = newContorl;
}

void eth_wait_link(MIIEthernet_t *eth) {
    uint16_t currentStatus;
    eth->mgmtRegAddr = 17;
    do {
        currentStatus = eth->mgmtPort;
    } while(!(currentStatus & MII_SPECSTS_LINK_UP));
}