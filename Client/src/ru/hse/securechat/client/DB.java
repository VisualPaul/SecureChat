package ru.hse.securechat.client;

import ru.hse.securechat.ProtocolConstants;

import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DB {
    public static void storeInDatabase(LoginUser user) throws SQLException {
        try (PreparedStatement stmt = ClientMain.getConnection().prepareStatement(
                 "INSERT INTO login (name, public_key, private_key) VALUES (?, ?, ?)")) {
            stmt.setString(1, user.getName());
            stmt.setBytes(2, user.getKeyPair().getPublic().getEncoded());
            stmt.setBytes(3, user.getKeyPair().getPrivate().getEncoded());
            stmt.executeUpdate();
        }
        storeInDatabase(new User(user.getName(), user.getKeyPair().getPublic()));
    }
    public static void storeInDatabase(User user) throws SQLException {
        try (PreparedStatement stmt = ClientMain.getConnection().prepareStatement(
                "INSERT INTO users (name, public_key) VALUES (?, ?)")) {
            stmt.setString(1, user.getName());
            stmt.setBytes(2, user.getPublicKey().getEncoded());
            stmt.executeUpdate();
        }
    }

    public static LoginUser getLoginUser(String name) throws SQLException {
        try (PreparedStatement stmt = ClientMain.getConnection().prepareStatement(
                "SELECT public_key, private_key FROM login WHERE name = ?")){
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
