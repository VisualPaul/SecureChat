package ru.hse.securechat;

import java.io.Serializable;
import java.security.PublicKey;

public class RegisterRequest implements Serializable {
    public static final int CODE = 0;
    public static final int ALREADY_EXISTS = 1;
    public static final int OK = 0;
    private PublicKey publicKey;
    private String name;

    public RegisterRequest(PublicKey publicKey, String name) {
        this.publicKey = publicKey;
        this.name = name;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }
}
