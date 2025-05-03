#include "pol_request_factory.h"
#include <string.h>
extern "C" {
    #include "monocypher.h"
    }
#include <esp_random.h>

PoLRequest createSignedPoLRequest(const uint8_t sk[32], const uint8_t pk[32]) {
    PoLRequest req = {};

    req.flags = 0xA1;
    req.phone_id = 0xDEADBEEF12345678;
    req.beacon_id = 0xCAFEBABE;
    memcpy(req.nonce, "TestNonce1234567", POL_NONCE_SIZE); // 16 bytes
    memcpy(req.phone_pk, pk, POL_PK_SIZE);

    // Sign the request
    uint8_t buffer[1 + 8 + 4 + POL_NONCE_SIZE + POL_PK_SIZE];
    req.getSignedData(buffer);

    crypto_sign(req.phone_sig, sk, pk, buffer, sizeof(buffer));
    return req;
}
