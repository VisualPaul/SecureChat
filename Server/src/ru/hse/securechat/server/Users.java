package ru.hse.securechat.server;

import ru.hse.securechat.ProtocolConstants;

import javax.sql.rowset.serial.SerialBlob;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Users {
    private static Users object;
    private Users() {
    }
    public static Users getInstance() {
        if (object == null) {
            synchronized (Users.class) {
                if (object == null) {
                    object = new Users();
                }
            }
        }
        return object;
    }
    public boolean hasUser(String name) throws SQLException {
        try (PreparedStatement stmt = ServerMain.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM users WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet resultSet = stmt.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }
    public void addUser(String name, PublicKey publicKey) throws SQLException {
        try (PreparedStatement stmt = ServerMain.getConnection().prepareStatement(
                "INSERT INTO users (name, public_key) VALUES (?, ?)")) {
            stmt.setString(1, name);
            stmt.setBlob(2, new SerialBlob(publicKey.getEncoded()));
            stmt.executeUpdate();
        }
    }
    public int getUserId(String name) throws SQLException {
        try (PreparedStatement stmt = ServerMain.getConnection().prepareStatement(
                "SELECT id FROM users WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next())
                return -1;
            else
                return resultSet.getInt(1);
        }
    }
    public User getUser(String name) throws SQLException {
        try (PreparedStatement stmt = ServerMain.getConnection().prepareStatement(
                "SELECT id, public_key FROM users WHERE name = ?")) {
            stmt.setString(1, name);
            ResultSet resultSet = stmt.executeQuery();
            if (!resultSet.next())
                return null;
            int id = resultSet.getInt(1);
            Blob keyBlob = resultSet.getBlob(2);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBlob.getBytes(1, (int)keyBlob.length()));
            PublicKey publicKey;
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(ProtocolConstants.ALGORITHM);
                publicKey = keyFactory.generatePublic(publicKeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
            return new User(id, name, publicKey);
        }
    }
}
