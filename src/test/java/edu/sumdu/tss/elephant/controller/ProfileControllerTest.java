package edu.sumdu.tss.elephant.controller;

import edu.sumdu.tss.elephant.helper.DBPool;
import edu.sumdu.tss.elephant.helper.Keys;
import edu.sumdu.tss.elephant.helper.utils.StringUtils;
import edu.sumdu.tss.elephant.model.User;
import edu.sumdu.tss.elephant.model.UserService;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.util.ContextUtil;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.sql2o.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class ProfileControllerTest
{
    Connection connection;
    private static final String FIND_USER = "SELECT count(1) FROM users where login = :login";

    @BeforeAll
    void setUp() {
        Keys.loadParams(new File("config.properties"));
        connection = DBPool.getConnection().open();
    }
    @AfterEach
    void tearDown() {
        connection.close();
    }

    @Test
    @DisplayName("Test to change the current mail to a new one")
    void changeUserEmailTest () {
        // User initialization
        String password = "test";
        User user = UserService.newDefaultUser();
        user.setLogin(StringUtils.randomAlphaString(8) + "@example.com");
        user.setPassword(password);
        UserService.save(user);
        // Changing user email
        String anotherLogin = StringUtils.randomAlphaString(8) + "@example.com";
        user.setLogin(anotherLogin);
        UserService.save(user);
        // Verify that the user email was changed correctly
        int user_count = connection.createQuery(FIND_USER)
                .addParameter("login", anotherLogin).executeScalar(Integer.class);
        assertEquals(1, user_count, "User data has been changed");
    }
}
