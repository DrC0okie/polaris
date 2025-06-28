#ifndef ICOMMAND_H
#define ICOMMAND_H

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
    virtual void execute() = 0;
};

#endif  // ICOMMAND_H
