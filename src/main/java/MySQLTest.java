//package main.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

// THIS CLASS IS BASICALLY JUST FOR TESTING NOW
// in order to test the most basic functionality of the project 
// 1. change the <mainClass> tag --> MySQLTEST
// 2. follow the normal steps in the TESTING SHI document
// you should see a very rudimenatry 
// 1
// testuser
// testpass


// I do plan on going back and changing up the user table schema to not require passwords to play

public class MySQLTest {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/GameDB";
        String user = "testuser";
        String password = "123";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            System.out.println("Connected to database!");

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            while (rs.next()) {
                System.out.println(rs.getString("id")); // replace column_name
                System.out.println(rs.getString("username")); // replace column_name
                System.out.println(rs.getString("password")); // replace column_name
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
