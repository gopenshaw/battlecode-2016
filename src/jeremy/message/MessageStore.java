package jeremy.message;

import jeremy.util.BoundedQueue;

public class MessageStore {
    private final BoundedQueue<TimedMessage> messageQueue;

    public MessageStore(int storeSize) {
        messageQueue = new BoundedQueue<TimedMessage>(storeSize);
    }

    public void addMessage(Message message, int expirationRound) {
        messageQueue.add(new TimedMessage(message, expirationRound));
    }

    public Message getNextMessage(int currentRound) {
        if (messageQueue.isEmpty()) {
            return null;
        }

        TimedMessage nextMessage = messageQueue.remove();
        while (nextMessage.expirationRound < currentRound
                && nextMessage.wasBroadcast) {

            if (messageQueue.isEmpty()) {
                return null;
            }

            nextMessage = messageQueue.remove();
        }

        if (nextMessage != null
                && nextMessage.expirationRound != currentRound) {
            nextMessage.wasBroadcast = true;
            messageQueue.add(nextMessage);
        }

        return nextMessage == null ? null : nextMessage.message;
    }
}
