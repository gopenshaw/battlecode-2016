package team014;

import battlecode.common.*;

public class SignalUtil {
    private static final SignalType[] typeEncoding = {SignalType.PING, SignalType.PARTS, SignalType.ENEMY,
        SignalType.ID};

    private static final RobotType[] robotTypeEncoding = {RobotType.ARCHON, RobotType.BIGZOMBIE, RobotType.FASTZOMBIE,
            RobotType.GUARD, RobotType.RANGEDZOMBIE, RobotType.SCOUT, RobotType.SOLDIER, RobotType.STANDARDZOMBIE,
            RobotType.TTM, RobotType.TURRET, RobotType.VIPER, RobotType.ZOMBIEDEN };

    public static RobotType getRobotType(Signal signal) {
        //--For ID signals
        return robotTypeEncoding[signal.getMessage()[1]];
    }

    private static int encode(RobotType robotType) {
        int encoded = 0;
        while (robotTypeEncoding[encoded] != robotType) {
            encoded++;
        }

        return encoded;
    }

    public static SignalType getType(Signal signal) {
        int[] message = signal.getMessage();
        if (message == null) {
            return SignalType.BASIC;
        }

        int type = message[0] % 10;
        return typeEncoding[type];
    }

    public static RobotData getRobotData(Signal signal, MapLocation validLocation) {
        int[] message = signal.getMessage();
        return new RobotData(Serializer.decode(message[1] / 10000, validLocation),
                message[1] % 10000);
    }

    public static int getRoundNumber(Signal signal) {
        return signal.getMessage()[0] / 10;
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

    public static MapLocation getPartsLocation(Signal s, MapLocation validLocation) {
        return Serializer.decode(s.getMessage()[1], validLocation);
    }

    public static void broadcastEnemy(RobotInfo robot, int radius, int roundNumber, RobotController rc) throws GameActionException {
        rc.broadcastMessageSignal(encode(SignalType.ENEMY) + roundNumber * 10,
                encode(robot), //--LLLLLLHHH
                radius);
    }

    public static void broadcastID(RobotType type, int radius, RobotController rc) throws GameActionException {
        rc.broadcastMessageSignal(encode(SignalType.ID),
                encode(type),
                radius);
    }
}
