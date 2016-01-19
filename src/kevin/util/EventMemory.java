package kevin.util;

public class EventMemory {
    private final int[] memory;
    private final int firstRound;

    private final int RECENT = 80;

    private final Event[] eventEncoding = {
            Event.BROADCAST_ZOMBIES_DEAD,
            Event.ZOMBIES_DEAD_PROPOSED,
            Event.ZOMBIES_DEAD_DENIED,
            Event.ZOMBIE_SPOTTED
    };

    public EventMemory(int firstRound) {
        this.firstRound = firstRound;
        memory = new int[10];

        for (int i = 0; i < memory.length; i++) {
            memory[i] = -1;
        }
    }

    public void record(Event event, int currentRound) {
        memory[encode(event)] = currentRound;
    }

    public boolean hasMemory(int currentRound) {
        return firstRound + RECENT < currentRound;
    }

    public boolean happenedRecently(Event event, int currentRound) {
        return memory[encode(event)] + RECENT > currentRound;
    }

    private int encode(Event event) {
        int encoding = 0;
        while (eventEncoding[encoding] != event) {
            encoding++;
        }

        return encoding;
    }

    public boolean happedLastRound(Event event, int currentRound) {
        return memory[encode(event)] == currentRound - 1;
    }
}
