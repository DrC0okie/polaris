
#ifndef POL_CONSTANTS_H
#define POL_CONSTANTS_H

#include <cstddef>  // For size_t

// Common constants for the Proof-of-Location protocol

// Cryptographic sizes
constexpr size_t POL_NONCE_SIZE = 16;  // Size of the nonce in bytes
constexpr size_t POL_SIG_SIZE = 64;    // Size of the Ed25519 signature in bytes
constexpr size_t POL_PK_SIZE = 32;  // Size of the Ed25519 public key in bytes
constexpr size_t POL_SK_SIZE = 64;
#endif  // POL_CONSTANTS_H
