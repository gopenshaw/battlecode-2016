package jeremy;

import battlecode.common.*;
import jeremy.util.BoundedQueue;
import jeremy.util.DirectionUtil;
import jeremy.util.RobotUtil;
import jeremy.message.MessageParser;

public class Soldier extends Robot {
    private static final int SAFE_DISTANCE = 250;
    private Signal[] roundSignals;
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);
    private LocationCollection zombieDens = new LocationCollection(20);
    private RobotData zombieToAttack;
    private RobotInfo[] attackableEnemies;
    private MapLocation enemyLocation;
    private MapLocation zombieDen;
    private RobotInfo[] adjacentTeammates;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        readBroadcasts();
        senseRobots();
        shootZombies();
        shootEnemies();
        microAwayFromZombies();
        moveTowardZombieNotGettingCloser();
        moveTowardZombie();
        moveTowardDen();
        moveTowardEnemy();
        moveAwayFromArchon();
        updateZombieMemory();
        clearRubble();
        spread();
    }

    private void moveTowardEnemy() throws GameActionException {
        if (enemyLocation == null
                || !rc.isCoreReady()) {
            return;
        }

        if (currentLocation.distanceSquaredTo(enemyLocation) > SAFE_DISTANCE) {
            tryMoveToward(enemyLocation);
        }
    }

    private void spread() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (adjacentTeammates.length > 3) {
            tryMove(DirectionUtil.getDirectionAwayFrom(adjacentTeammates, currentLocation));
        }
    }

    private void shootEnemies() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableEnemies.length == 0) {
            return;
        }

        RobotInfo lowestHealthEnemy = RobotUtil.getLowestHealthRobot(attackableEnemies);
        if (lowestHealthEnemy == null) {
            return;
        }

        rc.attackLocation(lowestHealthEnemy.location);
    }

    private void readBroadcasts() {
        roundSignals = rc.emptySignalQueue();
        zombieToAttack = getZombieToAttack();
        RobotData zombieDen = getZombieDen();
        if (zombieDen != null) {
            zombieDens.add(zombieDen.location);
        }

        MapLocation newEnemyLocation = getEnemyLocation();
        if (newEnemyLocation != null) {
            enemyLocation = newEnemyLocation;
        }
    }

    private void moveTowardDen() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombieDen == null
                && zombieDens.isEmpty()) {
            return;
        }

        if (zombieDen == null
                && !zombieDens.isEmpty()) {
            zombieDen = zombieDens.removeClosestTo(currentLocation);
        }

        if (rc.canSenseLocation(zombieDen)
                && rc.senseRobotAtLocation(zombieDen) == null) {
            zombieDen = null;
            return;
        }

        if (currentLocation.distanceSquaredTo(zombieDen) > 8) {
            tryMoveToward(zombieDen);
        }
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

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        attackableEnemies = senseAttackableEnemies();
        nearbyZombies = senseNearbyZombies();
        adjacentTeammates = rc.senseNearbyRobots(2, team);
    }

    private void moveAwayFromArchon() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo archon = RobotUtil.getRobotOfType(adjacentTeammates, RobotType.ARCHON);
        if (archon != null) {
            tryMove(archon.location.directionTo(currentLocation));
        }
    }

    private void shootZombies() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableZombies.length == 0) {
            return;
        }

        RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(attackableZombies);
        if (lowestHealthZombie == null) {
            return;
        }

        rc.attackLocation(lowestHealthZombie.location);
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
            if (parser.getMessageType() == MessageType.ZOMBIE) {
                RobotData robotData = parser.getRobotData();
                if (robotData.type == RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
    }

    private MapLocation getEnemyLocation() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            if (parser.getMessageType() == MessageType.ENEMY) {
                return parser.getRobotData().location;
            }
        }

        return null;
    }
}
