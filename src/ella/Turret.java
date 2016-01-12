package ella;

import battlecode.common.*;
import ella.message.MessageParser;
import ella.util.RobotUtil;
import ella.util.ZombieUtil;

public class Turret extends Robot{
    private Signal[] roundSignals;
    private MapLocation requestLocation;
    private RobotInfo[] nearbyZombies;

    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        nearbyZombies = senseNearbyZombies();
        attackEnemiesAndZombies();
        requestSpace();
        spreadOut();
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

        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
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
                if (parser.getMessageType() == MessageType.ZOMBIE
                        && parser.isCurrent(roundNumber)) {
                    MapLocation zombieLocation = parser.getRobotData().location;
                    if (rc.canAttackLocation(zombieLocation)) {
                        rc.attackLocation(zombieLocation);
                        return;
                    }
                }
            }
        }
    }

    private void requestSpace() throws GameActionException {
        if (rc.getType() == RobotType.TTM) {
            return;
        }

        RobotInfo[] neighbors = rc.senseNearbyRobots(2, team);
        int turretArchonCount = RobotUtil.getCountOfType(neighbors, RobotType.TURRET);
        turretArchonCount += RobotUtil.getCountOfType(neighbors, RobotType.ARCHON);
        if (turretArchonCount >= 7) {
            rc.broadcastSignal(2);
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

    private void spreadOut() throws GameActionException {
        if (rc.getType() == RobotType.TURRET
                && nearbyZombies.length > 0) {
            return;
        }

        if (requestLocation == null) {
            for (Signal s : roundSignals) {
                if (s.getTeam() == team
                        && s.getMessage() == null) {
                    requestLocation = s.getLocation();
                    rc.pack();
                    return;
                }
            }
        }

        if (requestLocation == null
                || !rc.isCoreReady()) {
            return;
        }

        if (rc.getType() == RobotType.TTM) {
            tryMove(requestLocation.directionTo(currentLocation));
            rc.unpack();
            requestLocation = null;
        }
    }
}
