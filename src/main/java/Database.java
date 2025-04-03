package main.java;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

