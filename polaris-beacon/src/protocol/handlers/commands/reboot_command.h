#ifndef REBOOT_COMMAND_H
#define REBOOT_COMMAND_H

#include "icommand.h"

class RebootCommand : public ICommand {
public:
    CommandResult execute() override;
};

#endif // REBOOT_COMMAND_H