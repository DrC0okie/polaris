#ifndef POL_REQUEST_FACTORY_H
#define POL_REQUEST_FACTORY_H

#include "pol_request.h"

// Creates a valid, signed PoLRequest using provided keypair
PoLRequest createSignedPoLRequest(const uint8_t sk[32], const uint8_t pk[32]);

#endif
