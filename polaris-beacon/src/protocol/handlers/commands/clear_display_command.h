#ifndef CLEAR_DISPLAY_COMMAND_H
#define CLEAR_DISPLAY_COMMAND_H

#include "icommand.h"
#include "utils/display_controller.h"

class ClearDisplayCommand : public ICommand {
public:
    explicit ClearDisplayCommand(DisplayController& displayController);
    void execute() override;

private:
    DisplayController& _displayController;
};

#endif  // CLEAR_DISPLAY_COMMAND_H