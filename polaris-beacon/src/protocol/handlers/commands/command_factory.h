#ifndef COMMAND_FACTORY
#define COMMAND_FACTORY

#include <ArduinoJson.h>

#include <memory>

#include "icommand.h"
#include "utils/led_controller.h"

class CommandFactory {
public:
    explicit CommandFactory(LedController& ledController);
    std::unique_ptr<ICommand> createCommand(OperationType opType, const JsonObject& params);

private:
    LedController& _ledController;
};

#endif  // COMMAND_FACTORY