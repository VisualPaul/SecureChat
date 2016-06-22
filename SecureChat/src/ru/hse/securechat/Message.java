package ru.hse.securechat;

public class Message {
    public static int CODE = 3;
    private final String from;
    private final String to;
    private final String message;

    public Message(String from, String to, String message) {
        this.from = from;
        this.to = to;
        this.message = message;
    }


    public String getMessage() {
        return message;
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }
}
