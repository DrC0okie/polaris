#include "utils.h"

#include <HardwareSerial.h>

void printHex(const uint8_t* data, size_t len) {
    Serial.print("HEX DUMP (");
    Serial.print(len);
    Serial.println(" bytes):");

    char line[16 * 3 + 1];  // Up to 16 bytes per line, 3 chars per byte +
                            // null terminator

    for (size_t i = 0; i < len; i += 16) {
        size_t chunk = (len - i >= 16) ? 16 : len - i;
        for (size_t j = 0; j < chunk; ++j) {
            sprintf(&line[j * 3], "%02X ", data[i + j]);
        }
        line[chunk * 3] = '\0';  // Null terminate
        Serial.println(line);
    }
}
