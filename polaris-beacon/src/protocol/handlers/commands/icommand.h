#ifndef ICOMMAND_H
#define ICOMMAND_H

#include <vector>
#include <cstdint>

struct CommandResult {
    bool success = true;
    std::vector<uint8_t> responsePayload;
};

/**
 * @interface ICommand
 * @brief Defines the interface for all executable command objects.
 *
 * Command Pattern, providing a single `execute` method that encapsulates a specific action.
 */
class ICommand {
public:
    virtual ~ICommand() = default;

    /** @brief Pure virtual method to be implemented by concrete commands. */
    virtual CommandResult execute() = 0;
};

#endif  // ICOMMAND_H
