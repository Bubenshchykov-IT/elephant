package edu.sumdu.tss.elephant.controller;

import edu.sumdu.tss.elephant.helper.DBPool;
import edu.sumdu.tss.elephant.helper.Keys;
import edu.sumdu.tss.elephant.helper.UserRole;
import edu.sumdu.tss.elephant.helper.enums.Lang;
import edu.sumdu.tss.elephant.helper.exception.NotFoundException;
import edu.sumdu.tss.elephant.helper.utils.StringUtils;
import edu.sumdu.tss.elephant.model.DbUserService;
import edu.sumdu.tss.elephant.model.User;
import edu.sumdu.tss.elephant.model.UserService;
import io.javalin.Javalin;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.Context;
import org.sql2o.Connection;

public class ProfileController extends AbstractController {

    public static final String BASIC_PAGE = "/profile";

    public ProfileController(Javalin app) {
        super(app);
    }

    public static void show(Context context) {
        context.render("/velocity/profile/show.vm", currentModel(context));
    }

    public static void language(Context context) {
        User user = currentUser(context);
        var lang = context.queryParam("lang");
        user.setLanguage(Lang.byValue(lang).toString());
        UserService.save(user);
        context.redirect(BASIC_PAGE);
    }

    public static void resetDbPassword(Context context) {
        User user = currentUser(context);
        //TODO add password validation
        JavalinLogger.info(user.toString());
        user.setDbPassword(context.formParam("db-password"));
        JavalinLogger.info(user.toString());
        UserService.save(user);
        DbUserService.dbUserPasswordReset(user.getUsername(), user.getDbPassword());
        context.sessionAttribute(Keys.INFO_KEY, "DB user password was changed");
        context.redirect(BASIC_PAGE);
    }

    public static void resetWebPassword(Context context) {
        User user = currentUser(context);
        //TODO add password validation
        user.password(context.formParam("web-password"));
        UserService.save(user);
        context.sessionAttribute(Keys.INFO_KEY, "Web user password was changed");
        context.redirect(BASIC_PAGE);
    }

    public static void resetApiPassword(Context context) {
        User user = currentUser(context);
        //TODO add password validation
        user.setPrivateKey(StringUtils.randomAlphaString(20));
        user.setPublicKey(StringUtils.randomAlphaString(20));
        UserService.save(user);
        context.sessionAttribute(Keys.INFO_KEY, "API keys was reset successful");
        context.redirect(BASIC_PAGE);
    }

    public static void upgradeUser(Context context) {
        User user = currentUser(context);
        user.setRole(UserRole.valueOf(context.formParam("role")).getValue());
        UserService.save(user);
        context.sessionAttribute(Keys.INFO_KEY, "Role has been changed");
        context.redirect(BASIC_PAGE);
    }


    private static final String USER_BY_LOGIN_SQL = "SELECT * FROM users WHERE login = :login";
    // The process of changing mail to a new one
    public static void changeUserEmail(Context context) {
        Connection con = DBPool.getConnection().open();
        User user = currentUser(context);
        String login = context.formParam("user-email");
        var emailUser = con.createQuery(USER_BY_LOGIN_SQL)
                .addParameter("login", login).executeAndFetchFirst(User.class);
        if (emailUser == null) {
            user.setLogin(context.formParam("user-email"));
            UserService.save(user);
            context.sessionAttribute(Keys.INFO_KEY, "User email was changed");
        }
        else context.sessionAttribute(Keys.ERROR_KEY, "Email is already in use. Enter another one");
        context.redirect(BASIC_PAGE);
    }

    public void register(Javalin app) {
        app.get(BASIC_PAGE + "/lang", ProfileController::language, UserRole.AUTHED);
        app.post(BASIC_PAGE + "/reset-password", ProfileController::resetWebPassword, UserRole.AUTHED);
        app.post(BASIC_PAGE + "/reset-db", ProfileController::resetDbPassword, UserRole.AUTHED);
        app.post(BASIC_PAGE + "/reset-api", ProfileController::resetApiPassword, UserRole.AUTHED);
        app.post(BASIC_PAGE + "/upgrade", ProfileController::upgradeUser, UserRole.AUTHED);
        app.post(BASIC_PAGE + "/change-email", ProfileController::changeUserEmail, UserRole.AUTHED);
        app.get(BASIC_PAGE, ProfileController::show, UserRole.AUTHED);
    }
}
