#ifndef POLARIS_PROTOCOL_H
#define POLARIS_PROTOCOL_H

#include <stdint.h>

#define POL_NONCE_SIZE 16
#define POL_SIG_SIZE   64
#define POL_PK_SIZE    32

struct PoLRequest {
  uint8_t flags;
  uint64_t phone_id;
  uint32_t beacon_id;
  uint8_t nonce[POL_NONCE_SIZE];
  uint8_t phone_pk[POL_PK_SIZE];
  uint8_t phone_sig[POL_SIG_SIZE];
};

struct PoLResponse {
  uint8_t flags;
  uint32_t beacon_id;
  uint64_t counter;
  uint8_t nonce[POL_NONCE_SIZE];
  uint8_t beacon_sig[POL_SIG_SIZE];
};


#endif
