package ru.hse.securechat;

import java.io.Serializable;

public class UserMessageSendRequest implements Serializable {
    public static final int CODE = 3;
    private int code;
    private String to;
    private byte[] message;

    public UserMessageSendRequest(int code, String to, byte[] message) {
        this.code = code;
        this.to = to;
        this.message = message;
    }

    public String getTo() {
        return to;
    }

    public byte[] getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
