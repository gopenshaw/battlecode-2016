package louie.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import louie.MessageType;

public class Serializer {
    //--Max 8 types
    private static final MessageType[] messageEncoding = {
            MessageType.PARTS, MessageType.ENEMY, MessageType.ID, MessageType.ZOMBIE,
            MessageType.ANNOUNCEMENT, MessageType.PAIR, MessageType.TARGET, MessageType.DESTROYED_DENS
    };

    //--Max 16 types
    private static final RobotType[] robotTypeEncoding = {RobotType.ARCHON, RobotType.BIGZOMBIE, RobotType.FASTZOMBIE,
            RobotType.GUARD, RobotType.RANGEDZOMBIE, RobotType.SCOUT, RobotType.SOLDIER, RobotType.STANDARDZOMBIE,
            RobotType.TTM, RobotType.TURRET, RobotType.VIPER, RobotType.ZOMBIEDEN };

    private static final AnnouncementSubject[] announcementSubjectEncoding = { AnnouncementSubject.ZOMBIES_DEAD };
    private static final AnnouncementMode[] announcementModeEncoding = { AnnouncementMode.PROPOSE,
        AnnouncementMode.DENY };

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

    public static int encode(AnnouncementSubject announcementSubject) {
        int encoded = 0;
        while (announcementSubjectEncoding[encoded] != announcementSubject) {
            encoded++;
        }

        return encoded;
    }

    public static AnnouncementSubject decodeAnnouncementSubject(int encoded) {
        return announcementSubjectEncoding[encoded];
    }

    public static AnnouncementMode decodeAnnouncementMode(int encoded) {
        return announcementModeEncoding[encoded];
    }

    public static int encode(AnnouncementMode announcementMode) {
        int encoded = 0;
        while (announcementModeEncoding[encoded] != announcementMode) {
            encoded++;
        }

        return encoded;
    }
}
