package ru.hse.securechat;

import java.io.Serializable;

public class UserMessage implements Serializable {
    public static int CODE = 3;
    private final String from;
    private final byte[] message;

    public UserMessage(String from, byte[] message) {
        this.from = from;
        this.message = message;
    }


    public byte[] getMessage() {
        return message;
    }

    public String getFrom() {
        return from;
    }
}
