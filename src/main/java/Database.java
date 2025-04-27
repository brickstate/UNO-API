//package main.java;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Handler;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/GameDB";
    private static final String USER = "testuser";
    // There is no password
    private static final String PASSWORD = "123";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void insertUser(String username, String password) throws Exception {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    public static List<User> getUsers() throws Exception {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
        ResultSet rs = stmt.executeQuery();
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password")));
        }
        rs.close();
        stmt.close();
        conn.close();
        return users;
    }
}
    /*  Some SQL Queries to make things happen

    Save User - INSERT INTO users (username, password, display_name) VALUES (?, ?, ?);

    Check if Username Exists - SELECT COUNT(*) FROM users WHERE username = ?;

    Save Current Game - INSERT INTO current_games (game_state) VALUES (?);

    Update Game State - UPDATE current_games SET game_state = ? WHERE game_id = ?;

    Delete Finished Game - DELETE FROM current_games WHERE game_id = ?;

    Archive Game to game_history - INSERT INTO game_history (game_id, players, winner, duration, game_log) VALUES (?, ?, ?, ?, ?);
    */

    // Call this from other classes like 
    // try (Connection conn = DatabaseUtil.getConnection()) {}

    /* Store JSON in Java
     * ObjectMapper objectMapper = new ObjectMapper();
     * String json = objectMapper.writeValueAsString(gameState); // save to DB
     * GameState loadedState = objectMapper.readValue(json, GameState.class); // load from DB
     */


    /* Add These in UnoServer
     * app.post("/createGame", createGame());
     * app.post("/joinGame", joinGame());
     * app.get("/gameState/:gameId", getGameState());
     * app.post("/playCard", playCard());
    */


    /* createGame()
        public static Handler createGame() {
            return ctx -> {
                try (Connection conn = DatabaseUtil.getConnection()) {
                    String initialStateJson = ;
            
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO current_games (game_state) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, initialStateJson);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int gameId = keys.getInt(1);
                    ctx.status(201).result("Game created with ID: " + gameId);
                } else {
                    ctx.status(500).result("Failed to create game");
                }
            }
        };
    }
     */

     /* joinGame()
      public static Handler joinGame() {
            return ctx -> {
                int gameId = Integer.parseInt(ctx.formParam("gameId"));
                String username = ctx.formParam("username");

                // Load game state JSON
                try (Connection conn = DatabaseUtil.getConnection()) {
                    PreparedStatement selectStmt = conn.prepareStatement(
                        "SELECT game_state FROM current_games WHERE game_id = ?");
                    selectStmt.setInt(1, gameId);
                    ResultSet rs = selectStmt.executeQuery();

                    if (rs.next()) {
                        String json = rs.getString("game_state");
                        ObjectMapper mapper = new ObjectMapper();
                        GameState game = mapper.readValue(json, GameState.class);

                        // Add player if not already in game
                        if (game.players.stream().noneMatch(p -> p.username.equals(username))) {
                            Player newPlayer = new Player(username); // or load full Player object
                            game.players.add(newPlayer);

                            String updatedJson = mapper.writeValueAsString(game);
                            PreparedStatement updateStmt = conn.prepareStatement(
                                "UPDATE current_games SET game_state = ? WHERE game_id = ?");
                            updateStmt.setString(1, updatedJson);
                            updateStmt.setInt(2, gameId);
                            updateStmt.executeUpdate();

                            ctx.result("Player added to game.");
                        } else {
                            ctx.result("Player already in game.");
                        }
                    } else {
                        ctx.status(404).result("Game not found.");
                    }
                }
            };
        }
      */


    /* getGameState()
         public static Handler getGameState() {
            return ctx -> {
                int gameId = Integer.parseInt(ctx.pathParam("gameId"));
                try (Connection conn = DatabaseUtil.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement(
                        "SELECT game_state FROM current_games WHERE game_id = ?");
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
     */

    /* playCard()
     public static Handler playCard() {
        return ctx -> {
            int gameId = Integer.parseInt(ctx.formParam("gameId"));
            String username = ctx.formParam("username");
            String cardJson = ctx.formParam("card"); // a card like {"color":"red","value":"5"}

            try (Connection conn = DatabaseUtil.getConnection()) {
                // Load game state
                PreparedStatement selectStmt = conn.prepareStatement(
                    "SELECT game_state FROM current_games WHERE game_id = ?");
                selectStmt.setInt(1, gameId);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.next()) {
                    ctx.status(404).result("Game not found.");
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                GameState game = mapper.readValue(rs.getString("game_state"), GameState.class);
                Card card = mapper.readValue(cardJson, Card.class);

                // Validate and apply the move
                GameLogic.playCard(game, username, card);

                // Save updated state
                String updatedJson = mapper.writeValueAsString(game);
                PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE current_games SET game_state = ? WHERE game_id = ?");
                updateStmt.setString(1, updatedJson);
                updateStmt.setInt(2, gameId);
                updateStmt.executeUpdate();

                ctx.result("Card played.");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Failed to play card: " + e.getMessage());
            }
        };
    }
     */