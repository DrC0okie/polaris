#ifndef NOOP_COMMAND_H
#define NOOP_COMMAND_H

#include "icommand.h"

class NoOpCommand : public ICommand {
public:
    CommandResult execute() override;
};

#endif // NOOP_COMMAND_H