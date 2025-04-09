//package main.java;

import io.javalin.http.Handler;
import java.util.ArrayList;
import java.util.List;

public class UserController {
    private static List<User> users = new ArrayList<>();

    public static Handler registerUser = ctx -> {
        User user = ctx.bodyAsClass(User.class);
        Database.insertUser(user.username, user.password);

        ctx.status(201).json("User registered!");
    };

    public static Handler getUsers = ctx -> {
        ctx.json(Database.getUsers());
    };
}
