package francis.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import francis.MessageType;

public class Serializer {
    //--Max 8 types
    private static final MessageType[] messageEncoding = {MessageType.PARTS, MessageType.ENEMY,
            MessageType.ID, MessageType.ZOMBIE, MessageType.COUNT };

    //--Max 16 types
    private static final RobotType[] robotTypeEncoding = {RobotType.ARCHON, RobotType.BIGZOMBIE, RobotType.FASTZOMBIE,
            RobotType.GUARD, RobotType.RANGEDZOMBIE, RobotType.SCOUT, RobotType.SOLDIER, RobotType.STANDARDZOMBIE,
            RobotType.TTM, RobotType.TURRET, RobotType.VIPER, RobotType.ZOMBIEDEN };

    public static int encode(MapLocation location) {
        return (location.x % 1000) * 1000 + (location.y % 1000);
    }

    public static int encode(RobotType robotType) {
        int encoded = 0;
        while (robotTypeEncoding[encoded] != robotType) {
            encoded++;
        }

        return encoded;
    }

    public static int encode(MessageType messageType) {
        int encoded = 0;
        while (messageEncoding[encoded] != messageType) {
            encoded++;
        }

        return encoded;
    }

    public static MapLocation decodeMapLocation(int i, MapLocation validLocation) {
        int x = getValidCoordinate(i / 1000, validLocation.x);
        int y = getValidCoordinate(i % 1000, validLocation.y);
        return new MapLocation(x, y);
    }

    private static int getValidCoordinate(int threeDigits, int validCoordinate) {
        int candidate = (validCoordinate / 1000) * 1000 + threeDigits;

        int currentThreeDigits = validCoordinate % 1000;
        if (currentThreeDigits < 100) {
            if (Math.abs(candidate - validCoordinate) > 100) {
                candidate -= 1000;
            }
        }
        else if (currentThreeDigits < 900) {
            if (Math.abs(candidate - validCoordinate) > 100) {
                candidate += 1000;
            }
        }

        return candidate;
    }

    public static MessageType decodeMessageType(int encodedMessageType) {
        return messageEncoding[encodedMessageType];
    }

    public static RobotType decodeRobotType(int encodedRobotType) {
        return robotTypeEncoding[encodedRobotType];
    }
}
