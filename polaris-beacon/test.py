from nacl.signing import SigningKey, VerifyKey
import struct

# Create fake PoLRequest values
flags = 0xA1
phone_id = 0x1122334455667788
beacon_id = 0x00BADC0DE
nonce = b'TestNonce1234567'  # 16 bytes exactly

# Generate key pair
signing_key = SigningKey.generate()
verify_key = signing_key.verify_key
phone_pk = verify_key.encode()

# Build signed part
signed_part = struct.pack("<BQI", flags, phone_id, beacon_id) + nonce + phone_pk

# Create Ed25519 signature over the signed fields
signature = signing_key.sign(signed_part).signature  # <- correct: 64 bytes

# Final payload
payload = signed_part + signature  # Total = 61 + 64 = 125 bytes

# Verify length
assert len(payload) == 125, f"Wrong length: {len(payload)}"

# Print hex string
print(" ".join(f"{b:02X}" for b in payload))


# === Verification ===

# Simulate receiving the same payload
received_signed_part = payload[:61]
received_signature = payload[61:]

# Reconstruct public key object from raw bytes
verify_key_from_bytes = VerifyKey(phone_pk)

try:
    verify_key_from_bytes.verify(received_signed_part, received_signature)
    print("✅ Signature is valid!")
except Exception as e:
    print("❌ Signature is invalid:", str(e))
