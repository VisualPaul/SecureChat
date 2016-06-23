package ru.hse.securechat;

import java.io.Serializable;

/**
 * Created by paul on 23.06.16.
 */
public class UserMessageSendResponse implements Serializable {
    public static final int CODE = 7;
    public static final int OK = 0;
    public static final int NO_USER = 1;
    public static final int USER_OFFLINE = 2;
    private int code;
    private int status;

    public UserMessageSendResponse(int code, int status) {
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
