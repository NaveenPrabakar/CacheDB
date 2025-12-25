package cachedb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private static final String URL =
            "jdbc:mysql://localhost:3306/cachedb?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "defg5678@";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
