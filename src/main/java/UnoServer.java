//package main.java;


import io.javalin.Javalin;


public class UnoServer {
    public static void main(String[] args) {
        
        
        Javalin app = Javalin.create().start(7000);
        
        // Endpoint for testing
        app.post("/users/register", ctx -> {
            // Here you can access the body as a JSON object if you need it
            System.out.println(ctx.body()); // Print the request body
            ctx.result("User registered successfully!"); // Respond to the client
        });


        app.get("/", ctx -> ctx.result("Uno Game API is running!"));

        app.get("/hello", ctx -> ctx.result("Hello, world!"));

        //app.post("/users/register", UserController.registerUser);
        app.get("/users", UserController.getUsers);

        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Server error: " + e.getMessage());
        });
    }
}

