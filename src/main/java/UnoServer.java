package main.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.type.TypeReference;

import Game_Logic.Game;
import Game_Parts.Card;
import Game_Parts.Deck;
import Game_Parts.GameState;
import Game_Parts.Hand;
import Game_Parts.Player;
import Game_Parts.Types.Color;
import Game_Parts.Types.Value;
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
        "  (Invoke-WebRequest -Uri \"http://localhost:7000/help\").Content"));

        //simple hello endpoint (works)
        app.get("/hello", ctx -> ctx.result("Hello, world!"));
        
        //add more endpoints to the helpEndpoint() function as we add more game mechanics to the API (add new commands)
        app.get("/help", helpEndpoint());

        //this will get all of the users from the "users" table (works)
        app.get("/listUsers", listUsers()); 

        //adds a new user to the database (works)  
        app.post("/registerUser", registerUser());

        //creates a new cpu game to be played
        app.post("/createCPUGame", createCPUGame());

        //creates a game with players
        app.post("/createPlayerGame", createPlayerGame());

        //lets a user join the game
        app.post("/joinGame/{gameId}/{username}", joinGame());

        //grabs the game state
        app.get("/gameState/{gameId}", getGameState());

        //plays a card
        app.post("/playCard/{gameId}/{username}/{card}", playCard());
        
        // Moves old games into the Completed_Games table
        app.delete("/checkOldGames", checkOldGames());
        
        //shows information about [TopCard] [# of cards in p2 hand] [exact cards in p1 hand]
        app.get("/showTable/{gameId}/{username}", showTable());

        app.post("/drawCards", drawCards());

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

/*
*   to get fresh game ID
     Invoke-WebRequest -Uri "http://localhost:7000/playCard" 
* 
*/
public static Handler createCPUGame() {
    // ALSO extends draw 7 card init functionality to CPU hand 
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

                //auto setup of COMPUTER player2 in Hands
                PreparedStatement updateStmt2 = conn.prepareStatement(
                    "UPDATE Hands_In_Game SET Player_2 = ? WHERE Game_Id = ?"
                );
                updateStmt2.setString(1, "COMPUTER");
                updateStmt2.setInt(2, gameId);
                updateStmt2.executeUpdate();


                ctx.status(201).result("CPU Game created with ID: " + gameId);


                ////////////////
                //? ! deck initialization in DB HERE
                Deck deckz = new Deck();
                ArrayList<Card> cards = deckz.getDeck(); 
                String deckjson = deckz.convertDeckToNumberedJson(cards);
                Card topCard = deckz.peekDiscard();
                String topCardjson = deckz.convertDiscardToNumberedJson(topCard);

                
                // Handles deck init in DB
            String sql = "UPDATE Hands_In_Game SET Deck_Cards = ? WHERE Game_ID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, deckjson);
                stmt.setInt(2, gameId);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    ctx.status(200).result("Deck successfully shuffled for Game_ID " + gameId);
                } else {
                    ctx.status(404).result("No matching Game_ID " + gameId + " found.");
                }
            }
                // Handles TOP (only 1 card on init) Discard pile  
            sql = "UPDATE Hands_In_Game SET Top_Card = ? WHERE Game_ID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, topCardjson);
                stmt.setInt(2, gameId);
                int updated = stmt.executeUpdate();
                if (updated > 0) {
                    ctx.status(200).result("Top card successfully stored for Game_ID " + gameId);
                } else {
                    ctx.status(404).result("No matching Game_ID " + gameId + " found.");
                }
            }


            /////////////////////// START TESTING [draw 7 cards init CPU Player2] //////////////////////////////
            // cpu 7 card hand init HERE
            //ObjectMapper mapper = new ObjectMapper();         //'mapper' already a local function

            //grab the shuffled json draw pile
            Map<String, String> drawdeck = mapper.readValue(deckjson, LinkedHashMap.class);

            // Sort keys numerically (since JSON keys are strings)
        List<Integer> sortedKeys = drawdeck.keySet().stream()
            .map(Integer::parseInt)
            .sorted()
            .collect(Collectors.toList());

        // Extract first 7 cards
        ObjectNode drawnJSON = mapper.createObjectNode();
        ObjectNode updatedJSON = mapper.createObjectNode();

        ObjectNode reindexedUpdatedJSON = mapper.createObjectNode();
        int newIndex = 1;

        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = String.valueOf(sortedKeys.get(i));
            if (i < 7) {
                drawnJSON.put(key, drawdeck.get(key));
            } else {
                updatedJSON.put(key, drawdeck.get(key));
            }
        }

        //important for updating draw deck keys
        for (Iterator<String> it = updatedJSON.fieldNames(); it.hasNext(); ) {
            String oldKey = it.next();
            String value = updatedJSON.get(oldKey).asText();
            reindexedUpdatedJSON.put(String.valueOf(newIndex++), value);
        }

        Map<String, String> result = new HashMap<>();
        result.put("drawn", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(drawnJSON));
        result.put("updated", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reindexedUpdatedJSON));
        //how to access drawn and updated cards 
        //result.get("drawn");   //result.get("updated");
        //PUSHING drawn cards (P1_Hand) and UpdatedDeck to DB !!
        String updateDeck = "UPDATE Hands_In_Game SET Deck_Cards = ?, P2_Hand = ? WHERE Game_ID = ?";
        PreparedStatement pushDeck = conn.prepareStatement(updateDeck);
        pushDeck.setString(1, result.get("updated"));
        pushDeck.setString(2, result.get("drawn"));
        pushDeck.setInt(3, gameId);

        int rowsUpdated = pushDeck.executeUpdate();
                if (rowsUpdated > 0) {
                    ctx.status(200).result("COMPUTER (Player-2) successfully drew 7 Cards :  Game " + gameId);
                } else {
                    ctx.status(404).result("Game ID not found.");
                }
            //prepared statement UPDATE (COMPUTER) P2_Hand 


            /////////////////////// END  TESTING //////////////////////////////

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
    {//TODO MAKE SURE IT WORKS 
    
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        return ctx -> {
            int gameId = Integer.parseInt(ctx.pathParam("gameId"));
            String username = ctx.pathParam("username");


            // Load game state JSON
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) 
            {
                //need to update Hands table with gameId/username -->

                //first check gameId is valid created game
                 // Check if gameId exists
            String selectQuery = "SELECT * FROM Hands_In_Game WHERE Game_ID = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                    selectStmt.setInt(1, gameId);
                    ResultSet rs = selectStmt.executeQuery();
    
                    if (!rs.next()) {
                        ctx.status(404).result("Game not found.");
                        return;
                    }
    
                    String player1 = rs.getString("Player_1");
                    String player2 = rs.getString("Player_2");
                    String player3 = rs.getString("Player_3");
                    String player4 = rs.getString("Player_4");
    
        // TODO////// Player ? hand, draw 7 card on init ////////////////////////////////////////////////
                    // If Player1 is null, assign username
                    // checks other names if p1 is taken
                    if (player1 == null) 
                    {
                        // ASSUME there is no players in the game rn.. 
                        // init Deck for the whole game // init Top Card (Discard Pile) 
            
                //[fixed] FIX BUG why is json acting weird?
                // 
                ObjectMapper mapper = new ObjectMapper();

        String deckQuery = "SELECT Deck_Cards FROM Hands_In_Game WHERE Game_ID = ?";
        PreparedStatement deckstatment = conn.prepareStatement(deckQuery);
        deckstatment.setInt(1, gameId);

        String resultDeckjson = "";

        try (ResultSet rs2 = deckstatment.executeQuery()) {
            if (rs2.next()) {
                // Assuming Deck_Cards is stored as JSON or VARCHAR in MySQL
                //BUG HERE ??????????????
                // we use rs2 HERE and not rs !!!!
                resultDeckjson = rs2.getString("Deck_Cards");
            } else {
                resultDeckjson = "ERROR idk why.."; // or throw exception if game ID not found
            }
        }

        // Parse the input JSON into a LinkedHashMap to preserve order
        Map<String, String> drawdeck = mapper.readValue(resultDeckjson, LinkedHashMap.class);

        // Sort keys numerically (since JSON keys are strings)
        List<Integer> sortedKeys = drawdeck.keySet().stream()
            .map(Integer::parseInt)
            .sorted()
            .collect(Collectors.toList());

        // Extract first 7 cards
        ObjectNode drawnJSON = mapper.createObjectNode();
        ObjectNode updatedJSON = mapper.createObjectNode();

        ObjectNode reindexedUpdatedJSON = mapper.createObjectNode();
        int newIndex = 1;

        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = String.valueOf(sortedKeys.get(i));
            if (i < 7) {
                drawnJSON.put(key, drawdeck.get(key));
            } else {
                updatedJSON.put(key, drawdeck.get(key));
            }
        }

        //important for updating draw deck keys
        for (Iterator<String> it = updatedJSON.fieldNames(); it.hasNext(); ) {
            String oldKey = it.next();
            String value = updatedJSON.get(oldKey).asText();
            reindexedUpdatedJSON.put(String.valueOf(newIndex++), value);
        }

        Map<String, String> result = new HashMap<>();
        result.put("drawn", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(drawnJSON));
        result.put("updated", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reindexedUpdatedJSON));
        //how to access drawn and updated cards 
        //result.get("drawn");   //result.get("updated");
        //PUSHING drawn cards (P1_Hand) and UpdatedDeck to DB !!
        String updateDeck = "UPDATE Hands_In_Game SET Deck_Cards = ?, P1_Hand = ? WHERE Game_ID = ?";
        PreparedStatement pushDeck = conn.prepareStatement(updateDeck);
        pushDeck.setString(1, result.get("updated"));
        pushDeck.setString(2, result.get("drawn"));
        pushDeck.setInt(3, gameId);

        int rowsUpdated = pushDeck.executeUpdate();
                if (rowsUpdated > 0) {
                    ctx.status(200).result(username +" successfully drew 7 Cards :  Game " + gameId);
                } else {
                    ctx.status(404).result("Game ID not found.");
                }
 


                //// end of testing ////////////////////////////////////////////////////////////////
                        //Below is the logic for getting P1 Username into DB
                        String updateQuery = "UPDATE Hands_In_Game SET Player_1 = ? WHERE Game_ID = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) 
                        {
                            updateStmt.setString(1, username);
                            updateStmt.setInt(2, gameId);
                            updateStmt.executeUpdate();
                            ctx.result("User " + username + " successfully joined as Player1.");
                        }
                    } 
                    else if ( player1 != null && player2 == null)
                    {
                        String updateQuery = "UPDATE Hands_In_Game SET Player_2 = ? WHERE Game_Id = ?";

                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) 
                        {
                            updateStmt.setString(1, username);
                            updateStmt.setInt(2, gameId);
                            updateStmt.executeUpdate();
                            ctx.result("User " + username + " successfully joined as Player2.");
                        }
                    }
                    else if ( player1 != null && player2 != null && player3 == null)
                    {
                        String updateQuery = "UPDATE Hands_In_Game SET Player_3 = ? WHERE Game_Id = ?";

                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) 
                        {
                            updateStmt.setString(1, username);
                            updateStmt.setInt(2, gameId);
                            updateStmt.executeUpdate();
                            ctx.result("User " + username + " successfully joined as Player3.");
                        }
                    }
                    else if ( player1 != null && player2 != null && player3 != null && player4 == null)
                    {
                        String updateQuery = "UPDATE Hands_In_Game SET Player_4 = ? WHERE Game_Id = ?";

                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) 
                        {
                            updateStmt.setString(1, username);
                            updateStmt.setInt(2, gameId);
                            updateStmt.executeUpdate();
                            ctx.result("User " + username + " successfully joined as Player4.");
                        }
                    }
                    else
                    {
                        ctx.status(409).result("Player1, Player2, Player3, and Player4 slot already taken.");
                    }

                    //  initialize a 7 card hand for new joined user IF we actually implement 1v1v1v1



                }


/* 
                // begining of junk //////////////////////
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
                // end of junk ///////////////
                */

            }
            catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).result("Internal server error: " + e.getMessage());
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
    
                // Check if game exists
                if (!rs.next()) {
                    ctx.status(404).result("Game not found.");
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();

                GameState game = mapper.readValue(rs.getString("game_state"), GameState.class);
    
                
                PreparedStatement handsStmt = conn.prepareStatement(
                    "SELECT Player_1, Player_2, Player_3, Player_4, P1_Hand, P2_Hand, P3_Hand, P4_Hand, Top_Card FROM Hands_In_Game WHERE Game_ID = ?"
                );
                handsStmt.setInt(1, gameId);
                ResultSet handsRs = handsStmt.executeQuery();

                String json = null;
                String matchedColumn = null;
                String handColumn = null;
                String topCardJson = null;

                if (handsRs.next()) {
                    topCardJson = handsRs.getString("Top_Card");
                    for (int i = 1; i <= 4; i++) {
                        String playerColumn = "Player_" + i;
                        String name = handsRs.getString(playerColumn);
                        if (username.equals(name)) {
                            matchedColumn = playerColumn;
                            handColumn = "P" + matchedColumn.split("_")[1] + "_Hand";
                            json = handsRs.getString(handColumn);
                            break;
                        }
                    }
                }

                if (matchedColumn == null) {
                    ctx.status(404).result("Player not found in game.");
                    return;
                }
                
                // Apply the game logic
                
                // First, find the correct column (P1_Hand, P2_Hand...) for this username
                
                handsStmt.setInt(1, gameId);
                ResultSet slotRs = handsStmt.executeQuery();

                if (!slotRs.next()) 
                {
                    ctx.status(404).result("Game not found in Hands_In_Game.");
                    return;
                }

                // Put into playerHand

                Hand playerHand = null;

                Map<String, String> rawMap = mapper.readValue(json, new TypeReference<Map<String, String>>() {});
                
                ArrayList<Card> cardList = new ArrayList<>();
                for (String entry : rawMap.values()) 
                {
                    String[] parts = entry.split(" ");
                    Card card = new Card(Color.valueOf(parts[0]), Value.valueOf(parts[1]));
                    // card.color = Color.valueOf(parts[0]);
                    // card.value = Value.valueOf(parts[1]);
                    cardList.add(card);
                }
                

                // Put into Hand object
                playerHand = new Hand();
                playerHand.hand = cardList;

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

                Map<String, String> topCardMap = mapper.readValue(topCardJson, new TypeReference<Map<String, String>>() {});
                String topCardStr = topCardMap.values().iterator().next();
                Card topCard = Game.parseCardFromString(topCardStr);

                //Card topCard = mapper.readValue(topCardJson, Card.class);



                // Validate card

                Card playedCard = playerHand.hand.get(cardIndex - 1);
                playedCard = Game.playCard(playedCard, topCard);

                if (playedCard == null)
                {
                    ctx.status(404).result("Card is invalid.");
                    return;
                }

                // Change top card

                topCardJson = mapper.writeValueAsString(playedCard);
                PreparedStatement updateTopCardStmt = conn.prepareStatement(
                    "UPDATE Hands_In_Game SET Top_Card = ? WHERE Game_ID = ?"
                );
           
                updateTopCardStmt.setString(1, topCardJson);
                updateTopCardStmt.setInt(2, gameId);
                updateTopCardStmt.executeUpdate();

                // Remove from hand

                playerHand.hand.remove(cardIndex);

                Map<String, String> handMap = Game.handToMapFormat(playerHand);
                String updatedHandJson = mapper.writeValueAsString(handMap); // ✅ Use the Map


                // Update the hand in the corresponding column
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

                //TODO: check if hand is now empty

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
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/registerUser -Method POST -Body @{username='yourname';password='yourpass'} -ContentType \"application/x-www-form-urlencoded\"\n\n")

                .append("[POST] /createCPUGame  --> Creates a new game against CPU\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/createCPUGame -Method POST\n\n")

                .append("[POST] /createPlayerGame --> Creates a new multiplayer game\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/createPlayerGame -Method POST\n\n")

                .append("[POST] /joinGame       --> Join an existing game\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/joinGame/{gameId}/{username} -Method POST -ContentType \"application/x-www-form-urlencoded\"\n\n")

                .append("[GET]  /gameState/{gameId} --> Get current state of a game\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/gameState/1\n\n")

                .append("[POST] /playCard       --> Play a card in a game\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/playCard -Method POST -Body @{gameId='1';card='{\"color\":\"red\",\"value\":\"5\"}'} -ContentType \"application/x-www-form-urlencoded\"\n\n")

                .append("[DELETE] /checkOldGames --> Move old games to completed_games table\n")
                .append("    Command: Invoke-WebRequest -Uri http://localhost:7000/checkOldGames -Method DELETE\n\n")

                .append("[GET]  /help           --> Displays this help information\n")
                .append("    Command: Invoke-WebRequest http://localhost:7000/help\n");

                //TODO more endpoints [/joinGame/{gameId}/{username} | /createCPUGame | /showTable | /drawCard/{gameId}/{username}]

        ctx.result(helpText.toString());
    };
  }

  public static Handler checkOldGames() {
    return ctx -> {
    String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
    String user = "testuser";
    String password = "123";
    int gameTimeout = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
        // Select games older than 24 hours
        String selectOldGamesSql = "SELECT Game ID FROM Game_Playing WHERE Recent Update < ?";
        PreparedStatement selectStmt = conn.prepareStatement(selectOldGamesSql);
        long cutoffTime = new Date().getTime() - gameTimeout;
        selectStmt.setTimestamp(1, new Timestamp(cutoffTime));
        ResultSet rs = selectStmt.executeQuery();

        while (rs.next()) {
            int gameId = rs.getInt("Game_ID");
            String gameState = rs.getString("game_state");
            boolean isCpuGame = rs.getBoolean("Is_CPU_Game");

            // Insert the old game into Game_Completed
            String insertSql = "INSERT INTO Game_Completed (Game_ID, game_state, Is_CPU_Game, completed_at) VALUES (?, ?, ?, NOW())";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setInt(1, gameId);
            insertStmt.setString(2, gameState);
            insertStmt.setBoolean(3, isCpuGame);
            insertStmt.executeUpdate();

            // Delete the game from Game_Playing
            String deleteSql = "DELETE FROM Game_Playing WHERE game_id = ?";
            PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, gameId);
            deleteStmt.executeUpdate();

            // Delete the game from Hands_In_Game table
            String deleteHandsSql = "DELETE FROM Hands_In_Game WHERE game_id = ?";
            PreparedStatement deleteHandsStmt = conn.prepareStatement(deleteHandsSql);
            deleteStmt.setInt(1, gameId);
            deleteStmt.executeUpdate();

            //System.out.println("Moved game ID " + gameId + " to completed games.");
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    };
    }

    public static Handler showTable()
    {   // ASUMES ONLY TWO PLAYERS IN GAME ... HUMAN V. CPU
        
        //TODO show table should:
        //    + # cards in CPU hand
        //    + show json for top card
        //    + list cards json for hand where -->  PX_Hand == [username]
        
        return ctx -> {
            String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
            String user = "testuser";
            String password = "123";

            int gameId = Integer.parseInt(ctx.pathParam("gameId"));
            String username = ctx.pathParam("username");

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) 
            {
                String selectQuery = "SELECT * FROM Hands_In_Game WHERE Game_ID = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                    selectStmt.setInt(1, gameId);
                    ResultSet rs = selectStmt.executeQuery();
    
                    if (!rs.next()) {
                        ctx.status(404).result("Game not found.");
                        return;
                    }
                    
                    String p1_Hand = rs.getString("P1_Hand");
                    String p2_Hand = rs.getString("P2_Hand");
                    String top_card = rs.getString("Top_Card");
                    
                    //process # cards in COMPUTER hand
                    ObjectMapper mapper = new ObjectMapper();
                    int p2_num_cards = -1;  // p2_num_cards will be set by defailt to -1 ,, can use for error checking if needed
                    Map<String, String> handMap = mapper.readValue(p2_Hand, Map.class);
                    p2_num_cards = handMap.size();
                    
                    

                    //print  [TOP CARD](.append)[card in P1_Hand]
                    StringBuilder result = new StringBuilder();
                    result.append("(Player2) # of cards in hand:\n").append(p2_num_cards).append("\n")
                      .append("Top Card:\n").append(top_card).append("\n")
                      .append("(Player 1) Your cards in hand:\n").append(p1_Hand).append("\n");
                
                    ctx.status(200).result(result.toString());
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
                ctx.status(500).result("Internal server error: " + e.getMessage());
            }

        };
    }

    public static Handler drawCards()
    {
        //TODO draw cards 
        //   + turn p1_Hand -> Hand p1Hand = new Hand()
        //   + call isHandValid() --> return T || F;
        //   + WHILE ... !valid() --> (draw card --+> p1_Hand) || valid() --> (do nothing ,, no need to draw)  
        return ctx -> {
            String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
            String user = "testuser";
            String password = "123";

            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) 
            {

            int gameId = Integer.parseInt(ctx.pathParam("gameId"));
            String username = ctx.pathParam("username");

            ObjectMapper mapper = new ObjectMapper();

            //get p1_Hand json from db
            String selectQuery = "SELECT * FROM Hands_In_Game WHERE Game_ID = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setInt(1, gameId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    ctx.status(404).result("Game not found.");
                    return;
                }

                String p1HandJSON = rs.getString("P1_Hand");
                String drawdeckJSON = rs.getString("Deck_Cards");
                String topCardJSON = rs.getString("Top_Card");


            // Put p1HandJSON into playerHand
            Hand playerHand = null;
            
            Map<String, String> rawMap = mapper.readValue(p1HandJSON, new TypeReference<Map<String, String>>() {});
            
            ArrayList<Card> cardList = new ArrayList<>();
            for (String entry : rawMap.values()) 
            {
                String[] parts = entry.split(" ");
                Card card = new Card(Color.valueOf(parts[0]), Value.valueOf(parts[1]));
                // card.color = Color.valueOf(parts[0]);
                // card.value = Value.valueOf(parts[1]);
                cardList.add(card);
            }


            
            // Put into Hand object
            playerHand = new Hand();
            playerHand.hand = cardList;
            

            Map<String, String> topCardMap = mapper.readValue(topCardJSON, new TypeReference<Map<String, String>>() {});
            String topCardStr = topCardMap.values().iterator().next();
            Card topCard = Game.parseCardFromString(topCardStr);

            //see if the hand p1 has is valid ,, if not keep drawing a card
            Game processgame = new Game();
            Map<String, String> result = new HashMap<>();
            while(!processgame.handIsValid(playerHand, topCard))
            {
                // Parse both JSON strings to LinkedHashMap to maintain order
                LinkedHashMap<String, String> p1Hand = mapper.readValue(p1HandJSON, LinkedHashMap.class);
                LinkedHashMap<String, String> drawDeck = mapper.readValue(drawdeckJSON, LinkedHashMap.class);

                // Get first entry from handB
                Iterator<Map.Entry<String, String>> iter = drawDeck.entrySet().iterator();
                //if (!iter.hasNext()) return result; // handB is empty  maybe useful for error debuging 

                Map.Entry<String, String> firstEntryB = iter.next();
                String firstCardValue = firstEntryB.getValue();

                // Add to handA
                int newKey = p1Hand.size() + 1;
                p1Hand.put(String.valueOf(newKey), firstCardValue);

                // Remove from handB
                iter.remove();

                // Rebuild handB with renumbered keys starting from 1
                LinkedHashMap<String, String> newDrawDeck = new LinkedHashMap<>();
                int index = 1;
                for (String value : drawDeck.values()) {
                    newDrawDeck.put(String.valueOf(index++), value);
                }

                // Convert both maps back to JSON strings
                result.put("p1Hand", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(p1Hand));
                result.put("drawDeck", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newDrawDeck));

                String updateDeck = "UPDATE Hands_In_Game SET Deck_Cards = ?, P1_Hand = ? WHERE Game_ID = ?";
                PreparedStatement pushDeck = conn.prepareStatement(updateDeck);
                pushDeck.setString(1, result.get("drawDeck"));
                pushDeck.setString(2, result.get("p1Hand"));
                pushDeck.setInt(3, gameId);

                pushDeck.executeUpdate();

                ctx.status(200).result("CPU Game created with ID: " + gameId);


            }

            }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        };
    
    }
}
