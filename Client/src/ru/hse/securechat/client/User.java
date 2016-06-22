package ru.hse.securechat.client;

import java.security.PublicKey;

/**
 * Created by paul on 22.06.16.
 */
public class User {
    private String name;
    private PublicKey publicKey;

    public User(String name, PublicKey publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getName() {
        return name;
    }
}
