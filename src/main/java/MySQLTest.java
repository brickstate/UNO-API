package main.java;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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
