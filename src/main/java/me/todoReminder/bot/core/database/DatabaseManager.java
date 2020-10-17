package me.todoReminder.bot.core.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import me.todoReminder.bot.core.commands.CommandContext;
import me.todoReminder.bot.core.database.schemas.GuildSchema;
import me.todoReminder.bot.core.database.schemas.TodoList;
import me.todoReminder.bot.core.database.schemas.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;


public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    private Datastore datastore;
    private MongoClient mongoClient;


    private DatabaseManager() {
        mongoClient = MongoClients.create();
        datastore = Morphia.createDatastore(mongoClient, "todoReminderDB", MapperOptions.builder().storeEmpties(true).build());
        datastore.getMapper().mapPackage("me.todoReminder.bot.core.database.models");
        datastore.ensureIndexes();
    }

    public static DatabaseManager getInstance() {
        if(instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void shutdown() {
        mongoClient.close();
    }

    // Getters for the database
    public UserModel getUser(String userID) {
        Query<UserModel> query = datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID));

        if(query.first() != null) return query.first();
        else {
            UserModel userModel = new UserModel(userID, "t.");
            datastore.save(userModel);
            return userModel;
        }
    }

    public String getPrefix(String guildID) {
        Query<GuildSchema> query = datastore.find(GuildSchema.class)
                .filter(Filters.eq("guildID", guildID));

        if(query.first() != null) return query.first().getPrefix();
        else {
            GuildSchema guildSchema = new GuildSchema(guildID, "t.");
            datastore.save(guildSchema);
            return guildSchema.getPrefix();
        }
    }


    // Update database data
    public void setPrefix(String guildID, String prefix) {
        datastore.find(GuildSchema.class)
                .filter(Filters.eq("guildID", guildID))
                .update(UpdateOperators.set("prefix", prefix))
                .execute();
    }

    public void newList(String userID, String name) {
        datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID))
                .update(UpdateOperators.push("todoLists", new TodoList(name)))
                .execute();
    }

    public void deleteList(String userID, CommandContext ctx) {
        datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID))
                .update(UpdateOperators.pullAll("todoLists", Collections.singletonList(ctx.getSelectedList())))
                .execute();
    }

    public void addTodo(String userID, int listIndex, String todo) {
        datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID))
                .update(UpdateOperators.push("todoLists."+listIndex+".todos", todo))
                .execute();
    }

    public void removeTodo(String userID, int listIndex, String todo) {
        datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID))
                .modify(UpdateOperators.pullAll("todoLists."+listIndex+".todos", Collections.singletonList(todo)))
                .execute();
    }

    public void completeTodo(String userID, int listIndex, String todo) {
        datastore.find(UserModel.class)
                .filter(Filters.eq("userID", userID))
                .modify(UpdateOperators.pullAll("todoLists."+listIndex+".todos", Collections.singletonList(todo)), UpdateOperators.push("todoLists."+listIndex+".completed", todo))
                .execute();
    }
}