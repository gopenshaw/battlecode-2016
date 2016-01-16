package jeremy.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import jeremy.MessageType;

public class MessageBuilder {
    private MessageBuilder()
    {

    }

    //--Message format
    //--First integer : 1 bit open, 2 bit round number, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type
    public static Message buildZombieMessage(RobotInfo zombie, int roundNumber) {
//        int first = (roundNumber % 4 << 29) + ((int) zombie.health << 15) + zombie.ID;
//        int second = (Serializer.encode(zombie.location) << 7)
//                + (Serializer.encode(zombie.type) << 3)
//                + Serializer.encode(MessageType.ZOMBIE);
//        return new Message(first, second, MessageType.ZOMBIE);
        return buildRobotMessage(zombie.health, zombie.ID, zombie.type, zombie.location, MessageType.ZOMBIE);
    }

    public static Message buildEnemyMessage(RobotInfo enemy, int roundNumber) {
        int first = (roundNumber % 4 << 29) + ((int) enemy.health << 15) + enemy.ID;
        int second = (Serializer.encode(enemy.location) << 7)
                + (Serializer.encode(enemy.type) << 3)
                + Serializer.encode(MessageType.ENEMY);
        return new Message(first, second, MessageType.ENEMY);
    }

    public static Message buildIdMessage(double health, int id, RobotType type, MapLocation location) {
        return buildRobotMessage(health, id, type, location, MessageType.ID);
    }

    public static Message buildSpreadMessage(double health, int id, RobotType type, MapLocation location) {
        return buildRobotMessage(health, id, type, location, MessageType.SPREAD);
    }

    private static Message buildRobotMessage(double health, int id, RobotType robotType, MapLocation location, MessageType messageType) {
        int first = ((int) health << 15) + id;
        int second = (Serializer.encode(location) << 7)
                + (Serializer.encode(robotType) << 3)
                + (Serializer.encode(messageType));
        return new Message(first, second, MessageType.ID);
    }

    public static Message buildCountMessage(int count) {
        int first = count;
        int second =  Serializer.encode(MessageType.COUNT);
        return new Message(first, second, MessageType.COUNT);
    }
}
