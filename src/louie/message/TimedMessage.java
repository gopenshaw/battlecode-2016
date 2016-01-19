package louie.message;

public class TimedMessage {
    Message message;
    int expirationRound;
    boolean wasBroadcast;

    TimedMessage(Message message, int expirationRound) {
        this.message = message;
        this.expirationRound = expirationRound;
        this.wasBroadcast = false;
    }
}
