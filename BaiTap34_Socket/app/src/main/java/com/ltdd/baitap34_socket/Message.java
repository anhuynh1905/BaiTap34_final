package com.ltdd.baitap34_socket;
public class Message {
    public static final int TYPE_SENT = 0;
    public static final int TYPE_RECEIVED = 1;

    private String username;
    private String message;
    private int type; // To distinguish sent vs received if needed for styling

    public Message(String username, String message, int type) {
        this.username = username;
        this.message = message;
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public int getType() {
        return type;
    }
}