package kevin.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import kevin.DestroyedDenData;
import kevin.MessageType;
import kevin.RobotData;
import kevin.PartsData;

public class MessageParser {
    //--Message format
    //--First integer : 1 bit open, 2 bit round number, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type

    private final int first;
    private final int second;
    private final MapLocation validLocation;

    public MessageParser(int first, int second, MapLocation validLocation) {
        this.first = first;
        this.second = second;
        this.validLocation = validLocation;
    }

    public MessageType getMessageType() {
        return Serializer.decodeMessageType(second & 0x7);
    }

    public RobotData getRobotData() {
        int id = first & 0x7FFF;
        MapLocation location = Serializer.decodeMapLocation(second >>> 7, validLocation);
        int health = first >>> 15;
        RobotType type = Serializer.decodeRobotType((second >>> 3) & 0xF);
        return new RobotData(id, location, health, type);
    }

    public boolean isCurrent(int roundNumber) {
        return roundNumber % 4 == (first >>> 29) % 4;
    }

    public int getCount() {
        return first;
    }

    public AnnouncementMode getAnnouncementMode() {
        return Serializer.decodeAnnouncementMode((second >>> 3) & 0xF);
    }

    public AnnouncementSubject getAnnouncementSubject() {
        return Serializer.decodeAnnouncementSubject(first);
    }

    public PartsData getPartsData() {
        return new PartsData(Serializer.decodeMapLocation(first, validLocation));
    }

    public boolean pairs(RobotInfo robot) {
        return robot.ID == first;
    }

    public DestroyedDenData getDestroyedDens() {
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
}
