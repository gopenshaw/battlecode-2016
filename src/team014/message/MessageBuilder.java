package team014.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import team014.MessageType;

public class MessageBuilder {
    //--Robot message format (Enemy, Zombie, ID)
    //--First integer : 3 bits open, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type

    private int first;
    private int second;

    public void buildZombieMessage(RobotInfo zombie) {
        first = ((int) zombie.health << 15) + zombie.ID;
        second = (Serializer.encode(zombie.location) << 7)
                + (Serializer.encode(zombie.type) << 3)
                + Serializer.encode(MessageType.ZOMBIE);
    }

    //--Parts message format
    //--First integer : parts amount
    //--Second integer: 20 bit location, 3 bit message type
    public void buildPartsMessage(MapLocation location, int amount) {
        first = amount;
        second = (Serializer.encode(location) << 3)
                + Serializer.encode(MessageType.PARTS);
    }

    public void buildEnemyMessage(RobotInfo enemy) {
        first = ((int) enemy.health << 15) + enemy.ID;
        second = (Serializer.encode(enemy.location) << 7)
                + (Serializer.encode(enemy.type) << 3)
                + Serializer.encode(MessageType.ENEMY);
    }

    public void buildIdMessage(double health, int id, RobotType type, MapLocation location) {
        first = ((int) health << 15) + id;
        second = (Serializer.encode(location) << 7)
                + (Serializer.encode(type) << 3)
                + (Serializer.encode(MessageType.ID));
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }
}
