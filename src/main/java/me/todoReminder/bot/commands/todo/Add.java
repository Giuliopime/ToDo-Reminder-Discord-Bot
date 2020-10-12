package me.todoReminder.bot.commands.todo;

import me.todoReminder.bot.core.aesthetics.EmbedReplies;
import me.todoReminder.bot.core.commands.Command;
import me.todoReminder.bot.core.commands.CommandCategory;
import me.todoReminder.bot.core.commands.CommandContext;
import me.todoReminder.bot.core.database.DatabaseManager;

public class Add extends Command {
    public static final String name = "add",
            description = "Add a task to a ToDo list" +
                    "\nIf you have multiple ToDo lists you can quickly select one with the flag `--number`" +
                    "\nExample: `t.add upload YT video --1`",
            usage = "[task's text] (flags | See t.help add)";
    private static final CommandCategory category = CommandCategory.TODO;
    private static final boolean requiresArgs = true;
    private static final String[] aliases = {"a"};
    private static final boolean chooseList = true;

    public Add() {
        super(name, description, usage, category, requiresArgs, aliases, chooseList);
    }

    public void run(CommandContext ctx) {
        if(ctx.getTodoLists().get(ctx.getListIndex()).getTodos().size() >= 100) {
            ctx.sendMessage(EmbedReplies.warningEmbed().setDescription("You already have 100 todos in that list!").build());
            return;
        }
        DatabaseManager.getInstance().addTodo(ctx.getUser().getId(), ctx.getListIndex(), ctx.getArgs());

        ctx.sendMessage(EmbedReplies.infoEmbed().setDescription("ToDo added!").build());
    }
}