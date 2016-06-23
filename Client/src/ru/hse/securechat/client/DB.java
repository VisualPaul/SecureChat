package ru.hse.securechat.client;

import ru.hse.securechat.ProtocolConstants;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

public class DB {

    private static Connection getConnection(boolean autoCommit) throws SQLException {
        Connection res = DriverManager.getConnection("jdbc:sqlite:client.db");
        res.setAutoCommit(autoCommit);
        return res;
    }
    private static Connection getConnection() throws SQLException {
        return getConnection(false);
    }
    public static void storeInDatabase(LoginUser user) throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO login (name, public_key, private_key) VALUES (?, ?, ?)")) {
                stmt.setString(1, user.getName());
                stmt.setBytes(2, user.getKeyPair().getPublic().getEncoded());
                stmt.setBytes(3, user.getKeyPair().getPrivate().getEncoded());
                stmt.executeUpdate();
            }
            storeInDatabase(connection, new User(user.getName(), user.getKeyPair().getPublic()));
        }
    }
    public static void storeInDatabase(User user) throws SQLException {
        try (Connection connection = getConnection()) {
            storeInDatabase(connection, user);
            connection.commit();
        }
    }
    public static void storeInDatabase(Connection connection, User user) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (name, public_key) VALUES (?, ?)")) {
            stmt.setString(1, user.getName());
            stmt.setBytes(2, user.getPublicKey().getEncoded());
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public static synchronized LoginUser getLoginUser(String name) throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT public_key, private_key FROM login WHERE name = ?")) {
                stmt.setString(1, name);
                ResultSet set = stmt.executeQuery();
                if (!set.next())
                    return null;
                EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(set.getBytes(1));
                EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(set.getBytes(2));
                PublicKey publicKey;
                PrivateKey privateKey;
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance(ProtocolConstants.ALGORITHM);
                    publicKey = keyFactory.generatePublic(pubKeySpec);
                    privateKey = keyFactory.generatePrivate(privKeySpec);

                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                return new LoginUser(name, new KeyPair(publicKey, privateKey));
            }
        }
    }

    public static synchronized User getUser(String name) throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT public_key FROM users WHERE name = ?")) {
                stmt.setString(1, name);
                ResultSet set = stmt.executeQuery();
                if (!set.next())
                    return null;
                EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(set.getBytes(1));
                PublicKey publicKey;
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance(ProtocolConstants.ALGORITHM);
                    publicKey = keyFactory.generatePublic(pubKeySpec);
                } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                return new User(name, publicKey);
            }
        }
    }

    public static void deleteUser(String name)  throws SQLException {
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM users WHERE name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
            connection.commit();
        }
    }

    public static void deleteLoginUser(String name) throws SQLException{
        try (Connection connection = getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM login WHERE name = ?")) {
                stmt.setString(1, name);
                stmt.executeUpdate();

            }
            connection.commit();
        }
    }
}
