package ru.hse.securechat.client;
import java.io.Serializable;
import java.security.KeyPair;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoginUser implements Serializable {
    private String name;
    private KeyPair keyPair;

    public LoginUser(String name, KeyPair keyPair) {
        this.name = name;
        this.keyPair = keyPair;
    }

    public String getName() {
        return name;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
