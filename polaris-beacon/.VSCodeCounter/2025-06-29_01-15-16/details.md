# Details

Date : 2025-06-29 01:15:16

Directory c:\\Users\\timot\\Documents\\HEIG\\TB\\polaris\\polaris-beacon

Total : 65 files,  2823 codes, 1086 comments, 915 blanks, all 4824 lines

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [README.md](/README.md) | Markdown | 96 | 0 | 37 | 133 |
| [beacons.code-workspace](/beacons.code-workspace) | JSON with Comments | 30 | 0 | 0 | 30 |
| [platformio.ini](/platformio.ini) | Ini | 19 | 9 | 2 | 30 |
| [src/ble/beacon\_advertiser.cpp](/src/ble/beacon_advertiser.cpp) | C++ | 54 | 6 | 18 | 78 |
| [src/ble/beacon\_advertiser.h](/src/ble/beacon_advertiser.h) | C++ | 31 | 45 | 17 | 93 |
| [src/ble/ble\_manager.cpp](/src/ble/ble_manager.cpp) | C++ | 389 | 60 | 77 | 526 |
| [src/ble/ble\_manager.h](/src/ble/ble_manager.h) | C++ | 94 | 64 | 52 | 210 |
| [src/ble/characteristics/icharacteristic.h](/src/ble/characteristics/icharacteristic.h) | C++ | 13 | 24 | 8 | 45 |
| [src/ble/characteristics/indicate\_characteristic.cpp](/src/ble/characteristics/indicate_characteristic.cpp) | C++ | 53 | 0 | 11 | 64 |
| [src/ble/characteristics/indicate\_characteristic.h](/src/ble/characteristics/indicate_characteristic.h) | C++ | 23 | 25 | 11 | 59 |
| [src/ble/characteristics/write\_characteristic.cpp](/src/ble/characteristics/write_characteristic.cpp) | C++ | 55 | 1 | 10 | 66 |
| [src/ble/characteristics/write\_characteristic.h](/src/ble/characteristics/write_characteristic.h) | C++ | 33 | 24 | 14 | 71 |
| [src/ble/connectable\_advertiser.cpp](/src/ble/connectable_advertiser.cpp) | C++ | 50 | 8 | 15 | 73 |
| [src/ble/connectable\_advertiser.h](/src/ble/connectable_advertiser.h) | C++ | 17 | 29 | 11 | 57 |
| [src/main.cpp](/src/main.cpp) | C++ | 101 | 15 | 21 | 137 |
| [src/protocol/handlers/commands/blink\_led\_command.cpp](/src/protocol/handlers/commands/blink_led_command.cpp) | C++ | 12 | 1 | 2 | 15 |
| [src/protocol/handlers/commands/blink\_led\_command.h](/src/protocol/handlers/commands/blink_led_command.h) | C++ | 15 | 15 | 8 | 38 |
| [src/protocol/handlers/commands/clear\_display\_command.cpp](/src/protocol/handlers/commands/clear_display_command.cpp) | C++ | 9 | 0 | 3 | 12 |
| [src/protocol/handlers/commands/clear\_display\_command.h](/src/protocol/handlers/commands/clear_display_command.h) | C++ | 12 | 0 | 4 | 16 |
| [src/protocol/handlers/commands/command\_factory.cpp](/src/protocol/handlers/commands/command_factory.cpp) | C++ | 40 | 2 | 10 | 52 |
| [src/protocol/handlers/commands/command\_factory.h](/src/protocol/handlers/commands/command_factory.h) | C++ | 21 | 21 | 9 | 51 |
| [src/protocol/handlers/commands/display\_text\_command.cpp](/src/protocol/handlers/commands/display_text_command.cpp) | C++ | 22 | 1 | 5 | 28 |
| [src/protocol/handlers/commands/display\_text\_command.h](/src/protocol/handlers/commands/display_text_command.h) | C++ | 17 | 16 | 10 | 43 |
| [src/protocol/handlers/commands/icommand.h](/src/protocol/handlers/commands/icommand.h) | C++ | 8 | 7 | 4 | 19 |
| [src/protocol/handlers/commands/noop\_command.cpp](/src/protocol/handlers/commands/noop_command.cpp) | C++ | 5 | 0 | 2 | 7 |
| [src/protocol/handlers/commands/noop\_command.h](/src/protocol/handlers/commands/noop_command.h) | C++ | 8 | 0 | 3 | 11 |
| [src/protocol/handlers/commands/reboot\_command.cpp](/src/protocol/handlers/commands/reboot_command.cpp) | C++ | 7 | 0 | 1 | 8 |
| [src/protocol/handlers/commands/reboot\_command.h](/src/protocol/handlers/commands/reboot_command.h) | C++ | 8 | 0 | 3 | 11 |
| [src/protocol/handlers/commands/request\_status\_command.cpp](/src/protocol/handlers/commands/request_status_command.cpp) | C++ | 14 | 3 | 6 | 23 |
| [src/protocol/handlers/commands/request\_status\_command.h](/src/protocol/handlers/commands/request_status_command.h) | C++ | 15 | 18 | 6 | 39 |
| [src/protocol/handlers/commands/stop\_blink\_command.cpp](/src/protocol/handlers/commands/stop_blink_command.cpp) | C++ | 8 | 0 | 3 | 11 |
| [src/protocol/handlers/commands/stop\_blink\_command.h](/src/protocol/handlers/commands/stop_blink_command.h) | C++ | 12 | 0 | 4 | 16 |
| [src/protocol/handlers/data\_pull\_handler.cpp](/src/protocol/handlers/data_pull_handler.cpp) | C++ | 17 | 0 | 3 | 20 |
| [src/protocol/handlers/data\_pull\_handler.h](/src/protocol/handlers/data_pull_handler.h) | C++ | 15 | 23 | 7 | 45 |
| [src/protocol/handlers/encrypted\_message\_handler.cpp](/src/protocol/handlers/encrypted_message_handler.cpp) | C++ | 158 | 8 | 26 | 192 |
| [src/protocol/handlers/encrypted\_message\_handler.h](/src/protocol/handlers/encrypted_message_handler.h) | C++ | 36 | 37 | 19 | 92 |
| [src/protocol/handlers/imessage\_handler.h](/src/protocol/handlers/imessage_handler.h) | C++ | 10 | 10 | 4 | 24 |
| [src/protocol/handlers/outgoing\_message\_service.cpp](/src/protocol/handlers/outgoing_message_service.cpp) | C++ | 85 | 0 | 18 | 103 |
| [src/protocol/handlers/outgoing\_message\_service.h](/src/protocol/handlers/outgoing_message_service.h) | C++ | 33 | 52 | 20 | 105 |
| [src/protocol/handlers/token\_message\_handler.cpp](/src/protocol/handlers/token_message_handler.cpp) | C++ | 38 | 2 | 12 | 52 |
| [src/protocol/handlers/token\_message\_handler.h](/src/protocol/handlers/token_message_handler.h) | C++ | 18 | 21 | 7 | 46 |
| [src/protocol/messages/encrypted\_message.cpp](/src/protocol/messages/encrypted_message.cpp) | C++ | 120 | 12 | 26 | 158 |
| [src/protocol/messages/encrypted\_message.h](/src/protocol/messages/encrypted_message.h) | C++ | 35 | 61 | 25 | 121 |
| [src/protocol/messages/pol\_request.cpp](/src/protocol/messages/pol_request.cpp) | C++ | 47 | 2 | 22 | 71 |
| [src/protocol/messages/pol\_request.h](/src/protocol/messages/pol_request.h) | C++ | 24 | 32 | 16 | 72 |
| [src/protocol/messages/pol\_response.cpp](/src/protocol/messages/pol_response.cpp) | C++ | 46 | 2 | 17 | 65 |
| [src/protocol/messages/pol\_response.h](/src/protocol/messages/pol_response.h) | C++ | 33 | 37 | 15 | 85 |
| [src/protocol/pol\_constants.h](/src/protocol/pol_constants.h) | C++ | 43 | 26 | 26 | 95 |
| [src/protocol/transport/fragmentation\_header.h](/src/protocol/transport/fragmentation_header.h) | C++ | 17 | 22 | 12 | 51 |
| [src/protocol/transport/fragmentation\_transport.cpp](/src/protocol/transport/fragmentation_transport.cpp) | C++ | 130 | 15 | 27 | 172 |
| [src/protocol/transport/fragmentation\_transport.h](/src/protocol/transport/fragmentation_transport.h) | C++ | 31 | 57 | 22 | 110 |
| [src/protocol/transport/imessage\_transport.h](/src/protocol/transport/imessage_transport.h) | C++ | 10 | 18 | 4 | 32 |
| [src/utils/beacon\_counter.cpp](/src/utils/beacon_counter.cpp) | C++ | 41 | 2 | 15 | 58 |
| [src/utils/beacon\_counter.h](/src/utils/beacon_counter.h) | C++ | 25 | 40 | 19 | 84 |
| [src/utils/crypto\_service.cpp](/src/utils/crypto_service.cpp) | C++ | 102 | 0 | 14 | 116 |
| [src/utils/crypto\_service.h](/src/utils/crypto_service.h) | C++ | 27 | 63 | 11 | 101 |
| [src/utils/display\_controller.cpp](/src/utils/display_controller.cpp) | C++ | 54 | 1 | 13 | 68 |
| [src/utils/display\_controller.h](/src/utils/display_controller.h) | C++ | 22 | 40 | 15 | 77 |
| [src/utils/key\_manager.cpp](/src/utils/key_manager.cpp) | C++ | 152 | 7 | 32 | 191 |
| [src/utils/key\_manager.h](/src/utils/key_manager.h) | C++ | 44 | 43 | 29 | 116 |
| [src/utils/led\_controller.cpp](/src/utils/led_controller.cpp) | C++ | 52 | 1 | 14 | 67 |
| [src/utils/led\_controller.h](/src/utils/led_controller.h) | C++ | 23 | 33 | 16 | 72 |
| [src/utils/system\_monitor.cpp](/src/utils/system_monitor.cpp) | C++ | 11 | 3 | 5 | 19 |
| [src/utils/system\_monitor.h](/src/utils/system_monitor.h) | C++ | 11 | 12 | 4 | 27 |
| [test.py](/test.py) | Python | 22 | 10 | 13 | 45 |

[Summary](results.md) / Details / [Diff Summary](diff.md) / [Diff Details](diff-details.md)