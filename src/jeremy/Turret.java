package jeremy;

import battlecode.common.*;
import jeremy.Robot;
import jeremy.message.MessageParser;
import jeremy.util.ZombieUtil;

public class Turret extends Robot {
    private Signal[] roundSignals;
    private MapLocation requestLocation;
    private RobotInfo[] nearbyZombies;
    private int spacesMoved;
    private RobotInfo[] nearbyEnemies;

    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        attackEnemiesAndZombies();
        move();
    }

    private void move() throws GameActionException {
        if (rc.getType() == RobotType.TURRET) {
            spacesMoved = 0;
            if (nearbyZombies.length == 0
                    && nearbyEnemies.length == 0) {
                rc.pack();
                return;
            }
        }

        if (spacesMoved >= 5) {
            rc.unpack();
            return;
        }

        if (!rc.isCoreReady()) {
            return;
        }

        tryMove(getRandomDirection());
        spacesMoved++;
    }

    private void attackEnemiesAndZombies() throws GameActionException {
        if (rc.getType() == RobotType.TTM) {
            return;
        }

        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo zombieToAttack = getPriorityAttackableZombie(nearbyZombies);
        if (zombieToAttack != null) {
            rc.attackLocation(zombieToAttack.location);
            return;
        }

        if (nearbyEnemies.length > 0) {
            if (rc.canAttackLocation(nearbyEnemies[0].location)) {
                rc.attackLocation(nearbyEnemies[0].location);
                return;
            }
        }

        for (Signal s : roundSignals) {
            if (s.getTeam() == team) {
                int[] message = s.getMessage();
                if (message == null) {
                    continue;
                }

                MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
                if (parser.isCurrent(roundNumber)
                        && (parser.getMessageType() == MessageType.ZOMBIE
                            || parser.getMessageType() == MessageType.ENEMY)) {
                    MapLocation attackLocation = parser.getRobotData().location;
                    if (rc.canAttackLocation(attackLocation)) {
                        rc.attackLocation(attackLocation);
                        return;
                    }
                }
            }
        }
    }

    private RobotInfo getPriorityAttackableZombie(RobotInfo[] zombies) {
        RobotInfo zombieToAttack = null;
        int highestPriority = -1;
        for (RobotInfo r : zombies) {
            int priority = ZombieUtil.getAttackPriority(r.type);
            if (priority > highestPriority
                    && rc.canAttackLocation(r.location)) {
                zombieToAttack = r;
                highestPriority = priority;
            }
        }

        return zombieToAttack;
    }
}
