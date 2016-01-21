package nels;

import battlecode.common.*;
import nels.message.MessageParser;
import nels.util.BoundedQueue;
import nels.util.DirectionUtil;
import nels.util.LocationUtil;
import nels.util.RobotUtil;

public class Soldier extends Robot {
    private static final int SAFE_DISTANCE = 250;

    private Signal[] roundSignals;

    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] attackableEnemies;
    private RobotInfo[] adjacentTeammates;
    private RobotInfo[] nearbyEnemies;

    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);
    private LocationCollection zombieDens = new LocationCollection(20);
    private RobotData zombieToAttack;
    private MapLocation enemyLocation;
    private RobotData zombieDen;
    private boolean[] denDestroyed = new boolean[32001];
    private MapLocation helpLocation;
    private int helpLocationTurn = 0;
    private static final int IGNORE_HELP_TURNS = 3;
    private RobotInfo[] nearbyFriendlies;

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
        microAwayFromEnemies();
        moveTowardZombie();
        moveTowardDen();
        moveTowardEnemy();
        moveAwayFromArchon();
        updateZombieMemory();
        clearRubble();
        spread();
    }

    private void microAwayFromEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        int maxEnemies = 6;
        BoundedQueue<RobotInfo> enemiesCanAttackMe = RobotUtil.getEnemiesThatCanAttack(nearbyEnemies, currentLocation, maxEnemies);
        int canAttackMe = enemiesCanAttackMe.getSize();
        if (canAttackMe == 0) {
            return;
        }

        int canAttackEnemy = RobotUtil.countCanAttack(nearbyFriendlies, enemiesCanAttackMe) + 1;
        setIndicatorString(2, "can attack me: " + canAttackMe);
        setIndicatorString(2, "can attack attackers: " + canAttackEnemy);
        int advantage = 2;
        if (canAttackMe == 1
                && canAttackEnemy == 2) {
            return;
        }

        if (canAttackMe + advantage > canAttackEnemy) {
            rc.broadcastSignal(senseRadius * 2);
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation));
        }
    }

    private void moveTowardEnemy() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (helpLocation != null
                && helpLocationTurn + IGNORE_HELP_TURNS > roundNumber
                && currentLocation.distanceSquaredTo(helpLocation) > 2) {
            setIndicatorString(2, "try move to help location");
            tryMoveToward(helpLocation);

            return;
        }

        if (enemyLocation != null) {
            setIndicatorString(2, "try move to enemy location");
            tryMoveToward(enemyLocation);
        }
    }

    private void spread() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (adjacentTeammates.length > 3) {
            setIndicatorString(0, "spreading");
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
        RobotData broadcastDen = getZombieDen();
        if (broadcastDen != null
                && !denDestroyed[broadcastDen.id]
                && (zombieDen == null
                        || broadcastDen.location != zombieDen.location)) {
            if (zombieDen == null) {
                zombieDens.add(broadcastDen);
            }
            else if (currentLocation.distanceSquaredTo(broadcastDen.location)
                        < currentLocation.distanceSquaredTo(zombieDen.location)) {
                //--let broadcast den overwrite ours if it is closer
                zombieDens.add(zombieDen);
                zombieDen = broadcastDen;
            }
        }

        MapLocation newEnemyLocation = getEnemyLocation();
        if (newEnemyLocation != null) {
            setIndicatorString(0, "old enemy location " + enemyLocation);
            setIndicatorString(1, "new enemy location " + newEnemyLocation);
            enemyLocation = newEnemyLocation;
        }

        updateDestroyedDens();
        getClosestHelpLocation();
        getTurretBroadcasts(roundSignals);
    }

    private void getClosestHelpLocation() {
        int maxSignals = 4;
        BoundedQueue<Signal> broadcasts = getBroadcasts(roundSignals, maxSignals);
        if (broadcasts.isEmpty()) {
            return;
        }

        MapLocation[] helpLocations = getLocationsFromSignals(broadcasts);
        helpLocation = LocationUtil.findClosestLocation(helpLocations, currentLocation);
        helpLocationTurn = roundNumber;
    }

    private MapLocation[] getLocationsFromSignals(BoundedQueue<Signal> signals) {
        int count = signals.getSize();
        MapLocation[] locations = new MapLocation[count];
        for (int i = 0; i < count; i++) {
            locations[i] = signals.remove().getLocation();
        }

        return locations;
    }

    private BoundedQueue<Signal> getBroadcasts(Signal[] roundSignals, int maxSignals) {
        BoundedQueue<Signal> broadcasts = new BoundedQueue<Signal>(maxSignals);
        for (Signal s : roundSignals) {
            if (s.getTeam() == team
                    && s.getMessage() == null) {
                broadcasts.add(s);
                if (broadcasts.isFull()) {
                    return broadcasts;
                }
            }
        }

        return broadcasts;
    }

    private Signal getFirstBroadcast(Signal[] signals) {
        for (Signal s : signals) {
            if (s.getTeam() == team
                    && s.getMessage() == null) {
                return s;
            }
        }

        return null;
    }

    private void updateDestroyedDens() {
        int[][] messages = getMessagesOfType(roundSignals, MessageType.DESTROYED_DENS);
        int messageCount = messages.length;
        for (int i = 0; i < messageCount; i++) {
            if (messages[i] == null) {
                break;
            }

            DestroyedDenData denData = MessageParser.getDestroyedDens(messages[i][0], messages[i][1]);
            for (int j = 0; j < denData.numberOfDens; j++) {
                int currentId = denData.denId[j];
                if (!denDestroyed[currentId]) {
                    denDestroyed[currentId] = true;
                    zombieDens.remove(currentId);
                }
            }
        }
    }

    private void moveTowardDen() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (nearbyEnemies.length > 0
                && RobotUtil.anyCanAttack(nearbyEnemies)) {
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

        if (rc.canSenseLocation(zombieDen.location)
                && rc.senseRobotAtLocation(zombieDen.location) == null) {
            denDestroyed[zombieDen.id] = true;
            zombieDen = null;
            return;
        }

        if (currentLocation.distanceSquaredTo(zombieDen.location) > 8) {
            if (roundNumber < 500) {
                tryMoveToward(zombieDen.location);
            }
            else {
                trySafeMoveToward(zombieDen.location, enemyTurrets);
            }
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
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();
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

            if (MessageParser.matchesType(message, MessageType.ZOMBIE)) {
                RobotData robotData = MessageParser.getRobotData(message);
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

            if (MessageParser.matchesType(message, MessageType.ZOMBIE)) {
                RobotData robotData = MessageParser.getRobotData(message);
                if (robotData.type == RobotType.ZOMBIEDEN) {
                    return robotData;
                }
            }
        }

        return null;
    }

    private MapLocation getEnemyLocation() {
        int[] message = getFirstMessageOfType(roundSignals, MessageType.ENEMY);
        if (message != null) {
            return MessageParser.getRobotData(message).location;
        }

        return null;
    }
}
