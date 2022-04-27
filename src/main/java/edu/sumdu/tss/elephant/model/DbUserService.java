package edu.sumdu.tss.elephant.model;

import edu.sumdu.tss.elephant.helper.DBPool;
import edu.sumdu.tss.elephant.helper.Keys;
import edu.sumdu.tss.elephant.helper.utils.CmdUtil;
import edu.sumdu.tss.elephant.helper.utils.ParameterizedStringFactory;
import io.javalin.core.util.JavalinLogger;

import javax.validation.constraints.Null;
import java.awt.*;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class DbUserService {

    private static final ParameterizedStringFactory CREATE_USER_SQL = new ParameterizedStringFactory("CREATE USER :name WITH PASSWORD ':password' CONNECTION LIMIT 5 IN ROLE customer;");
    private static final ParameterizedStringFactory DELETE_USER_SQL = new ParameterizedStringFactory("DROP USER :name");

    public static void initUser(String username, String password) {
        //Create user
        System.out.println("Username: " + username);
        String createUserString = CREATE_USER_SQL.addParameter("name", username).addParameter("password", password).toString();
        System.out.println(createUserString);
        DBPool.getConnection().open().createQuery(createUserString, false).executeUpdate();
        //Create tablespace
        String path = UserService.userStoragePath(username);
        System.out.println("Tablespace path:" + path);
        UserService.createTablespace(username, path + File.separator + "tablespace");
    }

    //TODO: SQL injection here!
    private static final ParameterizedStringFactory RESET_USER_SQL = new ParameterizedStringFactory("ALTER USER :name WITH PASSWORD ':password'");

    public static void dbUserPasswordReset(String name, String password) {
        String query = RESET_USER_SQL.addParameter("name", name).addParameter("password", password).toString();
        JavalinLogger.info(query);
        DBPool.getConnection().open().createQuery(query, false).executeUpdate();
    }

    // DB lines to delete all information about the user
    private static final ParameterizedStringFactory DELETE_USER_TABLESPACE = new ParameterizedStringFactory("DROP TABLESPACE :name");
    private static final String DELETE_USER_FROM_TABLE = "delete from users where username=:username";
    private static final String DELETE_USER_DATABASE = "delete from databases where name=:name and owner=:owner";
    private static final String SELECT_USER_DATA = "select name from databases where owner=:owner";

    public static void dropUser(String username, double role) {
        var connection = DBPool.getConnection().open();
        for (int i = 0; i < role; i++) {
            // The process of selecting DB that are created by a given user
            Database database = connection.createQuery(SELECT_USER_DATA)
                .addParameter("owner", username)
                .executeAndFetchFirst(Database.class);
            // Deleting user databases
            if (database == null) break;
            DatabaseService.drop(database);
            // Deleting databases from a database data table
            connection.createQuery(DELETE_USER_DATABASE)
                    .addParameter("name", database.getName())
                    .addParameter("owner", username)
                    .executeUpdate();
        }
        // Deleting user from a user data table
        connection.createQuery(DELETE_USER_FROM_TABLE)
                .addParameter("username", username)
                .executeUpdate();
        connection.createQuery(DELETE_USER_TABLESPACE
                .addParameter("name", username).toString(), false)
                .executeUpdate();
        connection.createQuery(DELETE_USER_SQL
                .addParameter("name", username).toString(), false)
                .executeUpdate();
        File file = new File(UserService.userStoragePath(username));
        UserService.recursiveDelete(file);
    }
}
