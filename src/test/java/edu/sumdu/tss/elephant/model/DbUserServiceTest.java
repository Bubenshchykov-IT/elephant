package edu.sumdu.tss.elephant.model;

import edu.sumdu.tss.elephant.helper.DBPool;
import edu.sumdu.tss.elephant.helper.utils.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sql2o.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DbUserServiceTest {

    private static final String FIND_USER = "SELECT count(1) FROM pg_catalog.pg_user where usename = :username";
    private static final String FIND_SPACE = "SELECT count(1) FROM pg_catalog.pg_tablespace where spcname = :username;";
    private static final String FIND_DB = "SELECT count(1) from pg_catalog.pg_database join pg_authid on pg_database.datdba = pg_authid.oid  where rolname = :username;";
    private static final long USER_ROLE = 2;

    Connection con;

    @BeforeEach
    void setUp() {
        con = DBPool.getConnection().open();
    }

    @AfterEach
    void tearDown() {
        con.close();
    }

    @Test
    void initUser() {
        String password = "test";
        User user = UserService.newDefaultUser();
        user.setLogin(StringUtils.randomAlphaString(8) + "@example.com");
        user.setPassword(password);
        UserService.save(user);

        String username = user.getUsername();

        DbUserService.initUser(username, password);
        int user_count = con.createQuery(FIND_USER).addParameter("username", username).executeScalar(Integer.class);
        int ts_count = con.createQuery(FIND_SPACE).addParameter("username", username).executeScalar(Integer.class);
        int db_count = con.createQuery(FIND_DB).addParameter("username", username).executeScalar(Integer.class);
        assertEquals(1, user_count, "User created");
        assertEquals(1, ts_count, "tablespace created");
        assertEquals(1, db_count, "database");
    }

    @Test
    void dbUserPasswordReset() {
    }

    @Test
    @DisplayName("Test removing a user from all system")
    void dropUser()
    {
        // User initialization
        String password = "test";
        User user = UserService.newDefaultUser();
        user.setLogin(StringUtils.randomAlphaString(8) + "@example.com");
        user.setPassword(password);
        user.setRole(USER_ROLE);
        UserService.save(user);
        // Deleting need user
        String username = user.getUsername();
        double role = user.getRole();
        DbUserService.dropUser(username, role);
        // Verify that the user was deleted correctly
        int user_count = con.createQuery(FIND_USER).addParameter("username", username).executeScalar(Integer.class);
        int ts_count = con.createQuery(FIND_SPACE).addParameter("username", username).executeScalar(Integer.class);
        int db_count = con.createQuery(FIND_DB).addParameter("username", username).executeScalar(Integer.class);
        assertEquals(0, user_count, "The user has been removed");
        assertEquals(0, ts_count, "The tablespace has been removed");
        assertEquals(0, db_count, "The database has been removed");
    }
}