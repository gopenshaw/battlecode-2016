package jeremy_the_second;

import battlecode.common.*;
import jeremy_the_second.message.MessageParser;
import jeremy_the_second.util.BoundedQueue;
import jeremy_the_second.util.DirectionUtil;
import jeremy_the_second.util.RobotUtil;

public class Soldier extends Robot {
    private Signal[] roundSignals;
    private RobotInfo[] attackableZombies;
    private RobotInfo[] attackableEnemies;
    private RobotInfo[] nearbyZombies;
    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);
    private RobotData zombieDen;
    private RobotData zombieToAttack;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        readBroadcasts();
        senseEnemies();
        senseZombies();
        shootEnemies();
        shootZombies();
        microAwayFromEnemies();
        microAwayFromZombies();
        moveTowardZombieNotGettingCloser();
        moveTowardZombie();
        moveTowardDen();
        moveAwayFromArchon();
        updateZombieMemory();
    }

    private void microAwayFromEnemies() throws GameActionException {
        if (attackableEnemies.length == 0
                || !rc.isCoreReady()
                || !RobotUtil.anyCanAttack(attackableEnemies)) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(attackableEnemies, currentLocation));
    }

    private void readBroadcasts() {
        roundSignals = rc.emptySignalQueue();
        zombieToAttack = getZombieToAttack();
        if (zombieDen == null) {
            zombieDen = getZombieDen();
        }
    }

    private void moveTowardDen() throws GameActionException {
        if (zombieDen == null
                || !rc.isCoreReady()) {
            return;
        }

        if (rc.canSenseLocation(zombieDen.location)
                && rc.senseRobotAtLocation(zombieDen.location) == null) {
            zombieDen = null;
            return;
        }

        tryMoveToward(zombieDen.location);
    }

    private void updateZombieMemory() {
        forgetZombiesFromLastTurn();
        rememberZombiesFromThisTurn();
    }

    private void rememberZombiesFromThisTurn() {
        for (RobotInfo zombie : nearbyZombies) {
            zombieMemory.add(zombie.ID);
        }
    }

    private void forgetZombiesFromLastTurn() {
        zombieMemory.clear();
    }

    private void moveTowardZombieNotGettingCloser() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        for (RobotInfo zombie : nearbyZombies) {
            if (zombie.type != RobotType.BIGZOMBIE
                    && zombie.type != RobotType.ZOMBIEDEN
                    && sawZombieLastTurn(zombie)) {
                tryMoveToward(zombie.location);
                break;
            }
        }

    }

    private boolean sawZombieLastTurn(RobotInfo zombie) {
        return zombieMemory.contains(zombie.ID);
    }

    private void microAwayFromZombies() throws GameActionException {
        if (attackableZombies.length == 0
                || !rc.isCoreReady()
                || !RobotUtil.anyCanAttack(attackableZombies)) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(attackableZombies, currentLocation));
    }

    private void senseZombies() {
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
    }

    private void senseEnemies() {
        attackableEnemies = senseAttackableEnemies();
    }

    private void moveAwayFromArchon() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] adjacentTeammates = rc.senseNearbyRobots(2, team);
        RobotInfo archon = RobotUtil.getRobotOfType(adjacentTeammates, RobotType.ARCHON);
        if (archon != null) {
            tryMove(archon.location.directionTo(currentLocation));
        }
    }

    private void shootZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(attackableZombies);
        if (lowestHealthZombie == null) {
            return;
        }

        rc.attackLocation(lowestHealthZombie.location);
    }

    private void shootEnemies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo lowestHealthEnemey = RobotUtil.getLowestHealthRobot(attackableEnemies);
        if (lowestHealthEnemey == null) {
            return;
        }

        rc.attackLocation(lowestHealthEnemey.location);
    }

    private void moveTowardZombie() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombieToAttack == null) {
            return;
        }

        tryMoveToward(zombieToAttack.location);
   }


    private RobotData getZombieToAttack() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            setIndicatorString(0, " " + parser.getMessageType());
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                RobotData robotData = parser.getRobotData();
                if (robotData.type != RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
    }

    private RobotData getZombieDen() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            setIndicatorString(0, " " + parser.getMessageType());
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                RobotData robotData = parser.getRobotData();
                if (robotData.type == RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
    }
}
