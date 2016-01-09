package charlie;

import battlecode.common.*;

public class SignalUtil {
    private static final SignalType[] typeEncoding = {SignalType.PING, SignalType.PARTS, SignalType.ENEMY};

    public static SignalType getType(Signal signal) {
        int[] message = signal.getMessage();
        if (message == null) {
            return SignalType.BASIC;
        }

        int type = message[0];
        return typeEncoding[type];
    }

    public static RobotData getRobotData(Signal signal, MapLocation validLocation) {
        int[] message = signal.getMessage();
        return new RobotData(Serializer.decode(message[1] / 10000, validLocation),
                message[1] % 10000);
    }

    public static int encode(SignalType type) {
        for (int i = 0; i < typeEncoding.length; i++) {
            if (typeEncoding[i] == type) {
                return i;
            }
        }

        return -1;
    }

    private static int encode(RobotInfo robot) {
        return (int)robot.health + Serializer.encode(robot.location) * 10000;
    }

    public static void broadcastParts(MapLocation mapLocation, int radius, RobotController rc) throws GameActionException {
        rc.broadcastMessageSignal(encode(SignalType.PARTS),
                Serializer.encode(mapLocation),
                radius);
    }

    public static void broadcastEnemy(RobotInfo robot, int radius, RobotController rc) throws GameActionException {
        rc.broadcastMessageSignal(encode(SignalType.ENEMY),
                encode(robot),
                radius);
    }

    private static int buildData(SignalType type) {
        if (type == SignalType.PARTS) {
            return 1;
        }
        else if (type == SignalType.ENEMY) {
            return 2;
        }

        return 0;
    }
}
