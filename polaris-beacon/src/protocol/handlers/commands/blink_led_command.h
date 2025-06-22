#ifndef BLINK_LED_COMMAND_H
#define BLINK_LED_COMMAND_H

#include <ArduinoJson.h>
#include "icommand.h"
#include "utils/led_controller.h"

class BlinkLedCommand : public ICommand {
public:
    BlinkLedCommand(LedController& ledController, const JsonObject& params);
    void execute() override;

private:
    LedController& _ledController;
    float _frequency;
    uint32_t _color;
};

#endif // BLINK_LED_COMMAND_H