#ifndef REBOOT_COMMAND_H
#define REBOOT_COMMAND_H

#include "icommand.h"

class RebootCommand : public ICommand {
public:
    void execute() override;
};

#endif // REBOOT_COMMAND_H