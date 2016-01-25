package uber.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import uber.DestroyedDenData;
import uber.MessageType;
import uber.PartsData;
import uber.RobotData;

public class MessageParser {
    public static MessageType getMessageType(int first, int second) {
        return Serializer.decodeMessageType(second & 0x7);
    }

    public static RobotData getRobotData(int first, int second) {
        int id = first & 0x7FFF;
        MapLocation location = Serializer.decodeMapLocation(second >>> 7);
        int health = first >>> 15;
        RobotType type = Serializer.decodeRobotType((second >>> 3) & 0xF);
        return new RobotData(id, location, health, type);
    }

    public static boolean isCurrent(int first, int second, int roundNumber) {
        return roundNumber % 4 == (first >>> 29) % 4;
    }

    public static int getCount(int first, int second) {
        return first;
    }

    public static AnnouncementMode getAnnouncementMode(int first, int second) {
        return Serializer.decodeAnnouncementMode((second >>> 3) & 0xF);
    }

    public static Subject getSubject(int first, int second) {
        return Serializer.decodeAnnouncementSubject(first);
    }

    public static PartsData getPartsData(int first, int second) {
        return new PartsData(Serializer.decodeMapLocation(first));
    }

    public static boolean pairs(int first, int second, RobotInfo robot) {
        return robot.ID == first;
    }

    public static DestroyedDenData getDestroyedDens(int first, int second) {
        int numberOfDens = first & 0x7;
        DestroyedDenData denData = new DestroyedDenData(numberOfDens);
        if (numberOfDens == 1) {
            denData.denId[0] = (first >>> 18);
        }
        else if (numberOfDens == 2) {
            denData.denId[0] = (first >>> 18);
            denData.denId[1] = (first >>> 3) & 0x7FFF;
        }
        else if (numberOfDens == 3) {
            denData.denId[0] = (first >>> 18);
            denData.denId[1] = (first >>> 3) & 0x7FFF;
            denData.denId[2] = (second >>> 18);
        }
        else if (numberOfDens == 4) {
            denData.denId[0] = (first >>> 18);
            denData.denId[1] = (first >>> 3) & 0x7FFF;
            denData.denId[2] = (second >>> 18);
            denData.denId[3] = (second >>> 3) & 0x7FFF;
        }

        return denData;
    }

    public static boolean matchesType(int[] message, MessageType messageType) {
        return getMessageType(message[0], message[1]) == messageType;
    }

    public static RobotData getRobotData(int[] message) {
        return getRobotData(message[0], message[1]);
    }

    public static MapLocation getLocation(int[] message) {
        return Serializer.decodeMapLocation(message[1] >>> 7);
    }

    public static boolean shouldApproach(int[] message) {
        return (message[0] & 0xF0000000) != 0;
    }
}
