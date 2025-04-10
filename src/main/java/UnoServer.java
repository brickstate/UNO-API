package main.java;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import io.javalin.Javalin;
import io.javalin.http.Handler;


//This is the "Game Server" which in the grand scheme of the project should:
//  1. listen to http endpoint conenctions for user interaction


// How server works:
// If you just open up the sql proxy on your local machine and then run the "MySQLTest" Class
// The server will only do one querey and will time out after (?) ms
// Running the server to make the connections will continually allow the java application to communicate 
//   with the Cloud SQL Proxy in order to query further to the database

//Things to fully test (in loose order) 
// 1. Register a user [/register]
// 2. Display All usernames [/users] (should already be fully functionaly just need to make sure)
// 3. Disscuss and establish a standard way of getting the UnoServer to call the GameLOGIC functions
//    - Game LOGIC should handle all turn validation/ JSON outputs for Discard Pile, Draw Pile, 
//       Displaying Card Count in user hand
//    !!! for the "I have one card and didnt say uno so now i must draw x more cards" mechanic
//       == have some column in game state when they have claimed UNO or not (should make implementing the mechanic easier)
// 4. GameState information (challenging) 
//    - Starting the game [/startgame/newgame] || [startgame/validID/XXX]
//    - List game id
//      + from recently started games (where two playes are not INTO the play of the game)
//    - Join a game id (to play)
//      + check if that SPECIFIC game ID ended (players already played and you cant play a old game) 
//    - Verbose HELP dialouge incase you forget the endpoints [/help]
// 5. Other shi idk bruh 1->4 was all i could think about now

public class UnoServer {
    public static void main(String[] args) {

        //NOTE when we get this running in docker we may need to revisit "jdbcUrl"
        //    and change localhost -> ?docker.something?

        //Necessary for the server to connect to Cloud SQL Proxy
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        //yes I hardcoded the user and password to the DB idc 
        String user = "testuser";
        String password = "123";
    
        
        //Keeps server from timing out and listens to enpoints
        Javalin app = Javalin.create().start(7000);
        
        // get server running and test enpoints
        // "landing" endpoint
        //Include some " ... for help run ???" for verbose help dialog 
        app.get("/", ctx -> ctx.result("Uno Game API is running! \nFor detailed info on Uno Game API endpoints: \n--->  curl http://localhost:7000/help"));

        //simple hello endpoint (works)
        app.get("/hello", ctx -> ctx.result("Hello, world!"));
        
        //add more endpoints to the helpEndpoint() function as we add more game mechanics to the API
        app.get("/help", helpEndpoint());

        //this will get all of the users from the "users" table (works)
        app.get("/listUsers", listUsers()); 

        // adds a new user to the database (works)  
        app.post("/registerUser", registerUser());


        

        //Leaving this here Until I need to delete it 
        // Endpoint for testing
        //app.post("/users/register", ctx -> {
            // Here you can access the body as a JSON object if you need it
        //    System.out.println(ctx.body()); // Print the request body
        //    ctx.result("User registered successfully!"); // Respond to the client
        //});
        //app.post("/users/register", UserController.registerUser);
        //app.get("/users", UserController.getUsers);


        // should catch server errors
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500);
            ctx.result("Server error: " + e.getMessage());
        });


    }

    // TEST this function by adding more user names into the database
    //   and see if the user names will print out one after the other
    public static Handler listUsers(){

        return ctx -> {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        //needed for proper lambda return type
        StringBuilder result = new StringBuilder();

        //we want to do this to test the connection is working
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            System.out.println("Listing all registerd users from GameDB...");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            //this being outside of the while loop should only print this line ONCE
            // not every time a new user is gathered to display
            result.append("Listing USERS from (UNO) GameDB \n");

            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                //String passwordField = rs.getString("password");

                //need to build the string to return to the /testing endpoint
                result.append("ID: ").append(id)
                      .append(", Username: ").append(username)
                      .append("\n");
            }

            ctx.result(result.toString());



        } catch (Exception e) {
            e.printStackTrace();
        }
    };
  }

  public static Handler registerUser(){
    return ctx -> {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        String username = ctx.formParam("username");
        String userPassword = ctx.formParam("password");

        if (username == null) {
            ctx.status(400).result("Missing 'username'");
            return;
        }

        //checking to see if attemped registration of username is unique entry in table "users"
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            // Check if username already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    ctx.status(409).result("Username already exists.");
                    return;
                }
            }

        //actually inserting unique user information into table "users"
        String insertSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, userPassword);
            int rowsInserted = insertStmt.executeUpdate();

            if (rowsInserted > 0) {
                ctx.status(201).result("User added successfully.");
            } else {
                ctx.status(500).result("User insertion failed.");
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        ctx.status(500).result("Database error: " + e.getMessage());
    }
        /*
        //old way to insert new info
        try (Connection conn2 = DriverManager.getConnection(jdbcUrl, user, password)) {
            System.out.println("Inserting a new user into GameDB...");

            String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, userPassword);
                int rowsInserted = pstmt.executeUpdate();

                if (rowsInserted > 0) {
                    ctx.status(201).result("User added successfully.");
                } else {
                    ctx.status(500).result("User insertion failed.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Database error: " + e.getMessage());
        }
        */
    };
  }

  public static Handler helpEndpoint() {
    // verbose help dialogue which we can add too in order
    //    to not reference docs when testing endpoints / for UX

    return ctx -> {
        StringBuilder helpText = new StringBuilder();

        //Add more help dialogue here as we add endpoints
        helpText.append("UNO GAME API - HELP\n\n")
                .append("[GET]  /               --> Health check\n")
                .append("    Description: Confirms the API is running.\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/\n\n")

                .append("[GET]  /hello          --> Simple hello-world test\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/hello\n\n")

                .append("[GET]  /listUsers      --> Lists all users in the database\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/listUsers\n\n")

                .append("[POST] /registerUser   --> Registers a new user with username and password\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/registerUser -Method POST -Body \"username=[ENTERUSERNAME]\" -ContentType \"application/x-www-form-urlencoded\"\n\n")

                .append("[GET]  /help           --> Displays this help information\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/help\n");

        ctx.result(helpText.toString());
    };
  }





}

