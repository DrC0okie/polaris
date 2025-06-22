#ifndef STOP_BLINK_COMMAND_H
#define STOP_BLINK_COMMAND_H

#include "icommand.h"
#include "utils/led_controller.h"

class StopBlinkCommand : public ICommand {
public:
    explicit StopBlinkCommand(LedController& ledController);
    void execute() override;

private:
    LedController& _ledController;
};

#endif // STOP_BLINK_COMMAND_H