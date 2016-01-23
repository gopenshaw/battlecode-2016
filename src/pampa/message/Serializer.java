package pampa.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import pampa.MessageType;

public class Serializer {
    //--Max 8 types
    private static final MessageType[] messageEncoding = {
            MessageType.PARTS, MessageType.ENEMY, MessageType.ENEMY_TURRET, MessageType.ZOMBIE,
            MessageType.ANNOUNCEMENT, MessageType.PAIR, MessageType.TARGET, MessageType.DESTROYED_DENS
    };

    //--Max 16 types
    private static final RobotType[] robotTypeEncoding = {RobotType.ARCHON, RobotType.BIGZOMBIE, RobotType.FASTZOMBIE,
            RobotType.GUARD, RobotType.RANGEDZOMBIE, RobotType.SCOUT, RobotType.SOLDIER, RobotType.STANDARDZOMBIE,
            RobotType.TTM, RobotType.TURRET, RobotType.VIPER, RobotType.ZOMBIEDEN };

    private static final Subject[] SUBJECT_ENCODING = { Subject.ZOMBIES_DEAD };
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

    public static MapLocation decodeMapLocation(int i) {
        int x = i / 1000;
        int y = i % 1000;
        return new MapLocation(x, y);
    }

    public static MessageType decodeMessageType(int encodedMessageType) {
        return messageEncoding[encodedMessageType];
    }

    public static RobotType decodeRobotType(int encodedRobotType) {
        return robotTypeEncoding[encodedRobotType];
    }

    public static int encode(Subject subject) {
        int encoded = 0;
        while (SUBJECT_ENCODING[encoded] != subject) {
            encoded++;
        }

        return encoded;
    }

    public static Subject decodeAnnouncementSubject(int encoded) {
        return SUBJECT_ENCODING[encoded];
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
