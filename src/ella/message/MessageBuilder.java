package ella.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import ella.MessageType;

public class MessageBuilder {
    //--Message format
    //--First integer : 1 bit open, 2 bit round number, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type

    private int first;
    private int second;

    public void buildZombieMessage(RobotInfo zombie, int roundNumber) {
        first = (roundNumber % 4 << 29) + ((int) zombie.health << 15) + zombie.ID;
        second = (Serializer.encode(zombie.location) << 7)
                + (Serializer.encode(zombie.type) << 3)
                + Serializer.encode(MessageType.ZOMBIE);
    }

    public void buildEnemyMessage(RobotInfo enemy, int roundNumber) {
        first = (roundNumber % 4 << 29) + ((int) enemy.health << 15) + enemy.ID;
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

    public void buildCountMessage(int count) {
        first = count;
        second =  Serializer.encode(MessageType.COUNT);
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }
}
