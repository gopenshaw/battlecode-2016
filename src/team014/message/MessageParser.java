package team014.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import team014.MessageType;
import team014.PartsData;
import team014.RobotData;

public class MessageParser {
    //--Message format
    //--First integer : 3 bits open, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type
    //--TODO should we have round number??
    //----only really need round number parity (1 bit)

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

    //--Parts message format
    //--First integer : parts amount
    //--Second integer: 20 bit location, 3 bit message type
    public PartsData getPartsData() {
        MapLocation location = Serializer.decodeMapLocation(second >>> 3, validLocation);
        int amount = first;
        return new PartsData(location, amount);
    }
}
