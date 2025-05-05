# Polaris Beacon

**Status:** *Work in progress — not production-ready*
 **Board Target:** Adafruit QT Py ESP32-S3 (N4R2)
 **Framework:** Arduino (via PlatformIO)
 **Language:** C++

## Overview

**Polaris Beacon** is an embedded BLE application for ESP32-S3 boards. It implements a proof-of-location protocol over BLE using cryptographic signatures (Ed25519 via Monocypher.

This project is part of an experimental system — it is **not ready for production**, and **no support or contributions** will be accepted at this stage.

## Features

- BLE GATT Server exposing two characteristics:
  - **Write:** For receiving signed PoLRequests from clients.
  - **Indicate:** For sending signed PoLResponses back.
- Uses a FreeRTOS queue for asynchronous request processing.
- Modular cryptographic handlers decoupled from BLE logic.
- Ed25519-based signature verification using Monocypher.
- Well-structured code with clear separation of responsibilities.

## Getting Started

### Requirements

- [PlatformIO](https://platformio.org/) (installed via VSCode or CLI)
- Adafruit QT Py ESP32-S3 (N4R2) board
- USB cable (data-capable)
- Serial monitor (e.g., PlatformIO built-in terminal)

## PlatformIO Configuration

Defined in `platformio.ini`:

```ini
[env:adafruit_qtpy_esp32s3_n4r2]
platform = espressif32
board = adafruit_qtpy_esp32s3_n4r2
framework = arduino
monitor_speed = 115200
lib_deps = davylandman/Monocypher@^2.0.6
build_flags =
    -DARDUINO_USB_MODE=1
    -DARDUINO_USB_CDC_ON_BOOT=1
    -DARDUINO_USB_MSC_OFF
    -DBOARD_HAS_PSRAM
    -mfix-esp32-psram-cache-issue
    -DED25519_SHA512
```

### Build and Flash

```bash
# Build the firmware
pio run

# Upload to the board
pio run --target upload

# Open serial monitor
pio device monitor
```

### Monitor Output

When connected, the serial monitor should show logs for:

- BLE advertising
- MTU negotiation
- Connection and disconnection events
- Received and verified PoL requests
- Sent responses

## Project Structure

```
polaris-beacon/
├── src/
│   ├── main.cpp                 # Entry point
│   ├── ble/                    # BLE server implementation
│   ├── protocol/               # PoLRequest/PoLResponse structures
│   ├── utils/                  # Helpers like `counter`
│   └── ipol_request_processor.h  # Handler interface
├── include/                    # Shared headers (optional)
├── lib/                        # External libraries (if needed)
├── platformio.ini              # Build configuration

```

## Limitations & Disclaimer

**This repository is under active development.**

- Not guaranteed to be stable.
- BLE protocol and project structure will evolve.
- No contributions or support requests will be considered at this time.

## License

This project is distributed for educational and internal research purposes only. Licensing terms will be defined later.

## External resources

* [Adafruit QT Py ESP32-S3 (N4R2) board detail](https://www.adafruit.com/product/5700)
* [Adafruit documentation](https://learn.adafruit.com/adafruit-qt-py-esp32-s3/overview)
* [PlatformIO IDE](https://docs.platformio.org/en/latest/integration/ide/pioide.html)
* [PlatformIO monocypher library registry](https://registry.platformio.org/libraries/davylandman/Monocypher)
* [monocypher library manual](https://monocypher.org/manual/)

