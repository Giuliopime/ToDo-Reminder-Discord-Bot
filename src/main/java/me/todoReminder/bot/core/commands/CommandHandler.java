package me.todoReminder.bot.core.commands;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import me.todoReminder.bot.core.aesthetics.EmbedReplies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private static final ClassGraph CLASS_GRAPH = new ClassGraph().whitelistPackages("me.todoReminder.bot.commands");
    private static Map<String, Command> COMMANDS = new HashMap<>();
    private static Map<String, String> ALIASES = new HashMap<>();

    public static void registerCommands() {
        try(ScanResult result = CLASS_GRAPH.scan()) {
            // Loop trough all the classes of the .commands package (cls stands for class)
            for (ClassInfo cls : result.getAllClasses()) {
                // Get the instance of the class of the command
                Command command = (Command) cls.loadClass().getDeclaredConstructor().newInstance();

                // Add the command to the COMMAND map if absent, if not throw an exception
                if(COMMANDS.putIfAbsent(command.getName().toLowerCase(), command) != null)
                    throw new IllegalArgumentException("Duplicate command " + command.getName());

                // Register the aliases of the commands in the ALIASES map, throws an exception if the alias is already present in the map
                if(command.getAliases() != null) {
                    for(String alias: command.getAliases())
                        if(ALIASES.putIfAbsent(alias.toLowerCase(), command.getName().toLowerCase()) != null)
                            throw new IllegalArgumentException("Duplicate alias " + alias);
                }
            }
            LOGGER.info("Loaded all commands successfully!");
        } catch (Exception e) {
            throw new IllegalArgumentException("Bot commands loading error:\n" + e);
        }
    }

    public static String isCommand(String commandName) {
        Command command = COMMANDS.get(commandName.toLowerCase());
        if(command == null) {
            String alias = ALIASES.get(commandName.toLowerCase());
            if(alias != null) command = COMMANDS.get(alias);
        }
        return command != null ? command.getName().toLowerCase() : null;
    }

    public static Command getCommand(String commandName) {
        Command command = COMMANDS.get(commandName.toLowerCase());
        if(command == null) {
            String alias = ALIASES.get(commandName.toLowerCase());
            if(alias != null) command = COMMANDS.get(alias);
        }
        return command;
    }

    public static boolean checkArgs(CommandContext ctx) {
        Command command = COMMANDS.get(ctx.getCommandName());

        if(command.requiresArgs() && ctx.getArgs() == null) {
            ctx.sendMessage(EmbedReplies.commandErrorEmbed().setDescription("Invalid usage of the command.\nThe correct usage would be:\n" + command.getUsageExample(ctx.getPrefix())));
            return false;
        }
        return true;
    }

    public static void runCommand(CommandContext ctx) {
        Command command = COMMANDS.get(ctx.getCommandName());

        if(command != null) {
            try{
                command.run(ctx);
            } catch (Exception e) {
                ctx.sendMessage(EmbedReplies.commandErrorEmbed());
                LOGGER.error("The command '{}' threw an error", ctx.getCommandName(), e);
            }
        }
    }


    public static Command[] getCommands() {
        return COMMANDS.values().toArray(new Command[0]);
    }
}
