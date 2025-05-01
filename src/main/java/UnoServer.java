package main.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import io.javalin.Javalin;
import io.javalin.http.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;

import Game_Parts.Card;
import Game_Parts.GameState;
import Game_Parts.Player;
import Game_Parts.Deck;
import Game_Logic.Game;
import Game_Parts.Hand;


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
// 3. Discuss and establish a standard way of getting the UnoServer to call the GameLOGIC functions
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
        app.get("/", ctx -> ctx.result("Uno Game API is running! \nFor detailed info on Uno Game API endpoints: \n--->" +
        "  (Invoke-WebRequest -Uri \"http://localhost:7000/listUsers\").Content"));

        //simple hello endpoint (works)
        app.get("/hello", ctx -> ctx.result("Hello, world!"));
        
        //add more endpoints to the helpEndpoint() function as we add more game mechanics to the API
        app.get("/help", helpEndpoint());

        //this will get all of the users from the "users" table (works)
        app.get("/listUsers", listUsers()); 

        //adds a new user to the database (works)  
        app.post("/registerUser", registerUser());

        //creates a new cpu game to be played
        app.post("/createCPUGame", createCPUGame());

        //creates a game with players
        app.post("/createPlayerGame", createPlayerGame());

        //NEED TO COMPLETE (game logic connections to endpoints)
        app.post("/startGame", startGame());

        //lets a user join the game
        app.post("/joinGame", joinGame());

        //grabs the game state
        app.get("/gameState/{gameId}", getGameState());

        //plays a card
        app.post("/playCard/{gameId}/{username}/{card}", playCard());
        

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
    };
  }

  public static GameState generateNewGameState() 
  {
    GameState game = new GameState();
    //Deck deck = new Deck();
    game.gameId = 0; // Will be set later if needed
    game.players = new ArrayList<>();
    game.deck = null; // You can define this
    game.discardPile = null;
    game.currentTurn = 0;
    game.direction = "clockwise";
    game.activeEffect = null;
    game.chosenColor = null;
    game.isGameOver = false;
    game.winner = null;
    return game;
  }


  public static Handler createCPUGame() {
    String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
    String user = "testuser";
    String password = "123";

    return ctx -> {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            GameState newGame = generateNewGameState();

            ObjectMapper mapper = new ObjectMapper();
            String initialStateJson = mapper.writeValueAsString(newGame);

            // Step 1: Insert initial game and set is_cpu_game = true
            PreparedStatement insertStmt = conn.prepareStatement(
                "INSERT INTO Game_Playing (game_state, is_cpu_game) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            insertStmt.setString(1, initialStateJson);
            insertStmt.setBoolean(2, true);  // <-- set it to true here
            insertStmt.executeUpdate();

            

            // Step 2: Get generated game_id
            ResultSet keys = insertStmt.getGeneratedKeys();
            if (keys.next()) {
                int gameId = keys.getInt(1);

                // Step 3: Update GameState with correct ID
                newGame.gameId = gameId;

                // Step 4: Serialize again
                String updatedJson = mapper.writeValueAsString(newGame);

                // Step 5: Update the database record
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Game_Playing SET game_state = ? WHERE game_id = ?"
                );

                PreparedStatement insertStmt2 = conn.prepareStatement(
                    "INSERT INTO Hands_In_Game (Game_ID) VALUES (?)"
                );
                insertStmt2.setInt(1, gameId);
                insertStmt2.executeUpdate();

                updateStmt.setString(1, updatedJson);
                updateStmt.setInt(2, gameId);
                updateStmt.executeUpdate();

                ctx.status(201).result("CPU Game created with ID: " + gameId);
            } else {
                ctx.status(500).result("Failed to create game");
            }
        }
    };
}

    public static Handler createPlayerGame() 
    {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        return ctx -> {
                try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) 
                {
                    String initialStateJson = 
                    "{\"gameId\":0," +
                    "\"players\":[]," +
                    "\"deck\":{\"cards\":[]}," +
                    "\"discardPile\":[]," +
                    "\"currentTurn\":0," +
                    "\"direction\":\"clockwise\"," +
                    "\"activeEffect\":null," +
                    "\"chosenColor\":null," +
                    "\"isGameOver\":false," +
                    "\"winner\":null}";
                
                    GameState newGame = new GameState();
                    ObjectMapper mapper = new ObjectMapper();
                    initialStateJson = mapper.writeValueAsString(newGame);

                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Game_Playing (game_state) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, initialStateJson);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) 
                {
                    int gameId = keys.getInt(1);
                    ctx.status(201).result("Game created with ID: " + gameId);
                } 
                else 
                {
                    ctx.status(500).result("Failed to create game");
                }
            }
        };
    }

    public static Handler joinGame() 
    {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        return ctx -> {
            int gameId = Integer.parseInt(ctx.formParam("gameId"));
            String username = ctx.formParam("username");

            // Load game state JSON
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) 
            {
                PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT game_state FROM Game_Playing WHERE game_id = ?");
                selectStmt.setInt(1, gameId);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) 
                {
                    String json = rs.getString("game_state");
                    ObjectMapper mapper = new ObjectMapper();
                    GameState game = mapper.readValue(json, GameState.class);

                    // Add player if not already in game
                    if (game.players.stream().noneMatch(p -> p.username.equals(username))) 
                    {
                        Player newPlayer = new Player(username); // or load full Player object
                        game.players.add(newPlayer);

                        String updatedJson = mapper.writeValueAsString(game);
                        PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE Game_Playing SET game_state = ? WHERE game_id = ?");
                        updateStmt.setString(1, updatedJson);
                        updateStmt.setInt(2, gameId);
                        updateStmt.executeUpdate();

                        ctx.result("Player added to game.");
                    } 
                    else 
                    {
                        ctx.result("Player already in game.");
                    }
                } 
                else 
                {
                    ctx.status(404).result("Game not found.");
                }
            }
        };
    }



    public static Handler getGameState() 
    {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        return ctx -> {
            int gameId = Integer.parseInt(ctx.pathParam("gameId"));
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT game_state FROM Game_Playing WHERE game_id = ?");
                stmt.setInt(1, gameId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    ctx.result(rs.getString("game_state"));
                } else {
                    ctx.status(404).result("Game not found.");
                }
            }
        };
    }

    /*
     * 
     Invoke-WebRequest -Uri "http://localhost:7000/playCard" `
      -Method POST `
      -Body @{gameId=1; username="Player1"; card=2}
     * 
     */
    public static Handler playCard() {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";
    
        return ctx -> {
            int gameId = Integer.parseInt(ctx.pathParam("gameId"));
            String username = ctx.pathParam("username");
            int cardIndex = Integer.parseInt(ctx.pathParam("card")); // card is now a number (index)
    
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                // Load game state
                PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT game_state FROM Game_Playing WHERE game_Id = ?"
                );
                selectStmt.setInt(1, gameId);
                ResultSet rs = selectStmt.executeQuery();
    
                if (!rs.next()) {
                    ctx.status(404).result("Game not found.");
                    return;
                }
    
                ObjectMapper mapper = new ObjectMapper();
                GameState game = mapper.readValue(rs.getString("game_state"), GameState.class);
    
                // Now find the player
                Player player = game.players.stream()
                    .filter(p -> p.username.equals(username))
                    .findFirst()
                    .orElse(null);
    
                if (player == null) 
                {
                    ctx.status(404).result("Player not found in game.");
                    return;
                }
            
                Hand playerHand = null;
    
                // Apply the game logic
                
                // First, find the correct column (P1_Hand, P2_Hand...) for this username
                PreparedStatement findSlotStmt = conn.prepareStatement(
                "SELECT Player_1, Player_2, Player_3, Player_4 FROM Hands_In_Game WHERE Game_ID = ?"
                );
                findSlotStmt.setInt(1, gameId);
                ResultSet slotRs = findSlotStmt.executeQuery();

                if (!slotRs.next()) 
                {
                    ctx.status(404).result("Game not found in Hands_In_Game.");
                    return;
                }

                // Get player hand
                String handColumn = null;

                for (int i = 1; i <= 4; i++) 
                {
                    String playerName = slotRs.getString("Player_" + i);
                    if (username.equals(playerName)) 
                    {
                        handColumn = "P" + i + "_Hand";
                        String handJson = slotRs.getString(handColumn);
                        
                        //ObjectMapper mapper = new ObjectMapper();
                        playerHand = mapper.readValue(handJson, Hand.class);
                        break;
                    }
                }

                // Check index

                if (cardIndex < 0 || cardIndex >= playerHand.hand.size()) 
                {
                    ctx.status(400).result("Invalid card index.");
                    return;
                }

                // Make sure player is found

                if (handColumn == null) 
                {
                    ctx.status(404).result("Player not found in Hands_In_Game.");
                    return;
                }

                String topCardJson = slotRs.getString("TopCard");
                Card topCard = mapper.readValue(topCardJson, Card.class);

                // Validate card

                Card playedCard = playerHand.hand.get(cardIndex);
                playedCard = Game.playCard(playedCard, topCard);

                if (playedCard == null)
                {
                    ctx.status(404).result("Card is invalid.");
                    return;
                }

                // Change top card

                topCardJson = mapper.writeValueAsString(playedCard);
                PreparedStatement updateTopCardStmt = conn.prepareStatement(
                    "UPDATE Hands_In_Game SET TopCard = ? WHERE Game_ID = ?"
                );
                updateTopCardStmt.setString(1, topCardJson);
                updateTopCardStmt.setInt(2, gameId);
                updateTopCardStmt.executeUpdate();

                // Remove from hand

                playerHand.hand.remove(cardIndex);
                
                // Update the hand in the corresponding column
                String updatedHandJson = mapper.writeValueAsString(playerHand);
                String updateHandQuery = "UPDATE Hands_In_Game SET " + handColumn + " = ? WHERE Game_ID = ?";
                PreparedStatement updateHandStmt = conn.prepareStatement(updateHandQuery);
                updateHandStmt.setString(1, updatedHandJson);
                updateHandStmt.setInt(2, gameId);
                updateHandStmt.executeUpdate();

    
                // Save updated game state
                String updatedJson = mapper.writeValueAsString(game);
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE Game_Playing SET game_state = ? WHERE Game_ID = ?"
                );
                updateStmt.setString(1, updatedJson);
                updateStmt.setInt(2, gameId);
                updateStmt.executeUpdate();
    
                ctx.result("Card played successfully.");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to play card: " + e.getMessage());
            }
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

  //NEED TO COMB THROUGH THIS and try and get it working/ test the endpoint
  public static Handler startGame() {
    return ctx -> {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";
    
        String userIdsParam = ctx.formParam("userIds"); // e.g., "1,2,3"
        if (userIdsParam == null || userIdsParam.isEmpty()) {
            ctx.status(400).result("Missing 'userIds' parameter.");
            return;
        }
    
        String[] userIds = userIdsParam.split(",");
    
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            conn.setAutoCommit(false);
    
            // 1. Create new game
            PreparedStatement gameStmt = conn.prepareStatement(
                "INSERT INTO games (status, turn) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            gameStmt.setString(1, "IN_PROGRESS");
            gameStmt.setInt(2, Integer.parseInt(userIds[0])); // First user's ID as starting turn
            gameStmt.executeUpdate();
    
            ResultSet generatedKeys = gameStmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                conn.rollback();
                ctx.status(500).result("Failed to create game.");
                return;
            }
    
            int gameId = generatedKeys.getInt(1);
    
            // 2. Shuffle and insert full deck
            // (You would load from cards table and shuffle in Java, then insert into deck table)
    
            // 3. Deal cards to each user
            // Loop through userIds, draw 7 cards for each from deck, insert into hands table
    
            // 4. Draw top card to discard_pile
    
            conn.commit();
            ctx.status(201).result("Game started with ID: " + gameId);
    
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
            ctx.status(500).result("Error starting game: " + e.getMessage());
        }
    };
  }
}
