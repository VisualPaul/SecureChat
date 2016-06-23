package ru.hse.securechat;

import java.io.Serializable;

public class AddUserRequest implements Serializable {
    public static int CODE = 4;
    private int messageCode;
    private String userName;

    public AddUserRequest(int messageCode, String userName) {
        this.messageCode = messageCode;
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public int getMessageCode() {
        return messageCode;
    }
}
