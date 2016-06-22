package ru.hse.securechat;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    public static final int CODE = 1;
    public static final int OK = 0;
    public static final int NO_USER = 1;
    public static final int FAILED = 2;
    private String name;

    public LoginRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
