package ru.hse.securechat.server;

import java.io.Serializable;
import java.security.PublicKey;

public class User implements Serializable {
    private int id;
    private String name;
    private PublicKey publicKey;

    public User(int id, String name, PublicKey publicKey) {
        this.id = id;
        this.name = name;
        this.publicKey = publicKey;
    }

    public String getName() {
        return name;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
            return false;
        User other = (User)o;
        return other.id == this.id;
    }
}
