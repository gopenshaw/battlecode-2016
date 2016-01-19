package jeremy.message;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import jeremy.DestroyedDenData;
import jeremy.MessageType;
import jeremy.RobotData;

public class MessageBuilder {
    private MessageBuilder()
    {

    }

    //--Message format
    //--First integer : 1 bit open, 2 bit round number, 14 bit health, 15 bit id
    //--Second integer: 20 bit location, 4 bit robot type, 3 bit message type
    public static Message buildZombieMessage(RobotInfo zombie, int roundNumber) {
        return buildRobotMessage(zombie.health, zombie.ID, zombie.type, zombie.location, MessageType.ZOMBIE);
    }

    public static Message buildZombieMessage(RobotData zombie, int roundNumber) {
        return buildRobotMessage(zombie.health, zombie.id, zombie.type, zombie.location, MessageType.ZOMBIE);
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

    public static Message buildPartsMessage(MapLocation center) {
        int first = Serializer.encode(center);
        int second = Serializer.encode(MessageType.PARTS);
        return new Message(first, second, MessageType.PARTS);
    }

    private static Message buildRobotMessage(double health, int id, RobotType robotType, MapLocation location, MessageType messageType) {
        int first = ((int) health << 15) + id;
        int second = (Serializer.encode(location) << 7)
                + (Serializer.encode(robotType) << 3)
                + (Serializer.encode(messageType));
        return new Message(first, second, MessageType.ID);
    }

    public static Message buildAnnouncement(AnnouncementSubject announcementSubject, AnnouncementMode announcementMode) {
        int first = Serializer.encode(announcementSubject);
        int second = (Serializer.encode(announcementMode) << 3)
                + Serializer.encode(MessageType.ANNOUNCEMENT);
        return new Message(first, second, MessageType.ANNOUNCEMENT);
    }

    public static Message buildPairingMessage(RobotInfo robot) {
        int first = robot.ID;
        int second = Serializer.encode(MessageType.PAIR);
        return new Message(first, second, MessageType.PAIR);
    }

    public static Message buildTargetMessage(RobotInfo closest) {
        return buildRobotMessage(closest.health, closest.ID, closest.type, closest.location, MessageType.TARGET);
    }

    public static Message buildDestroyedDenMessage(DestroyedDenData denData) {
        int first = 0;
        int count = denData.numberOfDens;
        if (count > 1) {
            first = (denData.denId[0] << 18)
                    + denData.denId[1] << 3
                    + denData.numberOfDens;
        }
        else {
            first = (denData.denId[0] << 18)
                    + denData.numberOfDens;
        }

        int second = Serializer.encode(MessageType.DESTROYED_DENS);
        if (count == 3) {
            second += denData.denId[2] << 18;
        }
        else if (count == 4) {
            second += (denData.denId[2] << 18)
                    + (denData.denId[3] << 3);
        }

        return new Message(first, second, MessageType.DESTROYED_DENS);
    }
}
