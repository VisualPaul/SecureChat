package ru.hse.securechat;

import java.io.Serializable;
import java.security.PublicKey;

public class AddUserResponse implements Serializable {
    public static final int CODE = 5;
    public static final int OK = 0;
    public static final int NO_USER = 1;
    private int code;
    private int status;
    private PublicKey publicKey;

    public AddUserResponse(int code, int status, PublicKey publicKey) {
        this.code = code;
        this.status = status;
        this.publicKey = publicKey;
    }

    public int getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
