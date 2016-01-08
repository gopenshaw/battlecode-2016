package charlie;

import battlecode.common.Signal;

public class SignalUtil {
    public static SignalType getType(Signal signal) {
        int[] message = signal.getMessage();
        if (message == null) {
            return SignalType.BASIC;
        }

        return SignalType.PING;
    }

    public static int getRoundNumber(Signal signal) {
        int[] message = signal.getMessage();
        return message[1];
    }

    public static String toString(Signal s) {
        return "Ping from round " + getRoundNumber(s);
    }
}
