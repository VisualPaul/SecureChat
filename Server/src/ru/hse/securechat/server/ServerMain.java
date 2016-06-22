package ru.hse.securechat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static ru.hse.securechat.ProtocolConstants.PORT;

public class ServerMain {
    private static Connection connection = null;
    public static Connection getConnection() {
        return connection;
    }

    private static void processRequests() {
        try (ServerSocket sock = new ServerSocket(PORT)) {
            while (true) {
                new UserConnection(sock.accept()).start();
            }
        } catch (IOException exc) {
            System.err.println("connection error:");
            exc.printStackTrace();
        }
    }
    public static void main(String[] args) {
        try {
            Class.forName(DatabaseCredentials.JDBC_DRIVER).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            connection = DriverManager.getConnection(DatabaseCredentials.connectionString,
                    DatabaseCredentials.USER_NAME, DatabaseCredentials.PASSWORD);
            processRequests();
        } catch (SQLException e) {
            System.err.println("internal SQL error: ");
            e.printStackTrace();
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                System.err.println("cannot close the connection:");
                e.printStackTrace();
            }
        }
    }
}
