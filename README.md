# POLARIS: A secure, opportunistic Proof-of-Location system
POLARIS is an end-to-end system designed to generate, verify, and transport  secure, unforgeable Proof-of-Location (PoL) claims. It provides a framework for applications that require knowledge of a  mobile device's physical presence at a specific location and time, even  in environments with intermittent connectivity.

The system is built on an asynchronous architecture,  using BLE-enabled beacons, a Kotlin Multiplatform SDK for mobile  clients, and a central server.

## The problem

Standard location services like GPS are effective outdoors but fail indoors and, more importantly, provide no cryptographic proof of presence. A user  can easily spoof their location, making it unsuitable for  security-critical applications like:

- Validating a caregiver's visit to a patient's home.
- Granting access to secure facilities.
- Automating payments in public transport based on presence.
- Verifying physical presence for compliance or auditing purposes.

Polaris solves this by creating a trust anchor at a known location (the beacon) and a secure protocol for a mobile device to prove its proximity to that anchor.

## System overview

The Polaris ecosystem consists of three main components, all included in this repository:

1. [Polaris Beacons](polaris-beacon):
   - Low-cost, secure, ESP32-S3 based boards placed at fixed, known locations.
   - They engage in a challenge-response protocol with mobile devices to issue signed PoL Tokens.
   - They act as secure endpoints for an end-to-end encrypted channel with the server, working with mobile relays.
2. [Polaris SDK](polaris-app/polaris-sdk):
   - A Kotlin Multiplatform library for Android and iOS that provides a simple API for mobile applications.
   - It handles all the complexity of BLE communication, cryptographic operations, and network requests.
   - It allows an app to scan for beacons, perform PoL transactions, and act as an opportunistic "data mule," relaying encrypted messages between the  beacon and the server without having access to the content.
3. [Polaris server](polaris-server):
   - A backend built with Kotlin and Quarkus.
   - Acts as the central authority: registers devices, provisions beacon information, and validates submitted PoL tokens.
   - Orchestrates the asynchronous, end-to-end encrypted communication channel for remote beacon management.

## Features & concepts

- PoL tokens are double-signed by both the phone and the beacon,  timestamped with a monotonic counter, and include a nonce to prevent  replay attacks. The server can verify these claims with full confidence.
-  The system is designed to work in environments with poor or no internet connectivity. Mobile phones act as data mules, securely ferrying  encrypted commands and data between beacons and the server whenever they get a connection. This is inspired by Delay-Tolerant Networking (DTN)  principles.
-  All communication for beacon management (e.g., remote commands, status  updates) is end-to-end encrypted between the beacon and the server using a shared secret derived via an ECDH key exchange. The mobile phone relaying the message cannot decrypt or  tamper with its content.
- Each component is designed with a layered architecture, dependency  injection, and clear interfaces, making the system easy to maintain,  test, and extend.

## Repository structure

This is a monorepo containing the three core projects of the Polaris system:

- [/polaris-beacon](polaris-beacon/README.md): Firmware for the ESP32-S3 beacon.
- [/polaris-app](polaris-app/README.md): The Android studio project containing the SDK and a sample Android application.
- [/polaris-server](polaris-server/README.md): The Quarkus-based central server application.

Each project directory contains its own detailed README.md with specific instructions for setup, building, and usage.

## Getting Started

To explore the Polaris system, it is recommended to set up the components in the following order:

1. **Set up the Server:** Follow the instructions in the [/polaris-server/README.md](polaris-server/README.md) to launch the backend and its PostgreSQL database. This is your central point of control.
2. **Set up a Beacon:** Follow the instructions in the [/polaris-beacon/README.md](polaris-beacon/README.md) to flash the firmware onto an ESP32-S3 board. You will need to provision it with the server's public key.
3. **Run the Sample App:** Follow the instructions in the [/polaris-app/README.md](polaris-app/README.md) to build and run the sample Android application, which uses the SDK to interact with the beacon and the server.
