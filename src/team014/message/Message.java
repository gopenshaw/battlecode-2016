package team014.message;

import team014.MessageType;

public class Message {
    private int first;
    private int second;
    private MessageType type;

    Message(int first, int second, MessageType type) {
        this.first = first;
        this.second = second;
        this.type = type;
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    public MessageType getType() {
        return type;
    }
}
