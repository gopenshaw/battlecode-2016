package melody;

import battlecode.common.*;
import melody.message.MessageParser;
import melody.util.BoundedQueue;
import melody.util.DirectionUtil;
import melody.util.LocationUtil;
import melody.util.RobotUtil;

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
        setIndicatorString(1, "can attack me: " + canAttackMe);
        setIndicatorString(1, "can attack attackers: " + canAttackEnemy);
        int advantage = 2;
        if (canAttackMe + advantage > canAttackEnemy) {
            rc.broadcastSignal(senseRadius * 2);
            setBytecodeIndicator(0, "micro away from enemies");
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation));
        }
    }

    private void moveTowardEnemy() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (helpLocation != null
                && currentLocation.distanceSquaredTo(helpLocation) > 2) {
            setIndicatorString(0, "going to help location " + helpLocation);
            if (!trySafeMoveToward(helpLocation, enemyTurrets)) {
                tryMove(DirectionUtil.getDirectionAwayFrom(enemyTurrets, currentLocation));
            }

            return;
        }

        if (enemyLocation != null
                && currentLocation.distanceSquaredTo(enemyLocation) > 50) {
            setIndicatorString(0, "going to enemy location " + enemyLocation);
            if (!trySafeMoveToward(enemyLocation, enemyTurrets)) {
                tryMove(DirectionUtil.getDirectionAwayFrom(enemyTurrets, currentLocation));
            }
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
            zombieDens.add(broadcastDen);
        }

        MapLocation newEnemyLocation = getEnemyLocation();
        if (newEnemyLocation != null) {
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
        int maxDenMessages = 10;
        MessageParser[] parsers = getParsersForMessagesOfType(roundSignals, MessageType.DESTROYED_DENS, maxDenMessages);
        for (int i = 0; i < maxDenMessages; i++) {
            if (parsers[i] == null) {
                break;
            }

            DestroyedDenData denData = parsers[i].getDestroyedDens();
            for (int j = 0; j < denData.numberOfDens; j++) {
                int currentId = denData.denId[j];
                if (!denDestroyed[currentId]) {
                    denDestroyed[currentId] = true;
                    zombieDens.remove(currentId);
                    setIndicatorString(1, " destroyed " + currentId);
                }
            }
        }
    }

    private void moveTowardDen() throws GameActionException {
        if (nearbyEnemies.length > 0
                || !rc.isCoreReady()) {
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
            setIndicatorString(0, "going to den " + zombieDen);
            Direction direction = currentLocation.directionTo(zombieDen.location);
            if (!trySafeMove(direction, enemyTurrets)) {
                tryMove(DirectionUtil.getDirectionAwayFrom(enemyTurrets, currentLocation));
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
                setIndicatorString(0, "move toward zombie not getting closer " + zombie.location);
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

        setIndicatorString(0, "micro away from zombies");
        tryMove(DirectionUtil.getDirectionAwayFrom(attackableZombies, currentLocation));
    }

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        attackableEnemies = senseAttackableEnemies();
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        setIndicatorString(2, "nearby enemy count: " + nearbyEnemies.length);
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
            setIndicatorString(0, "move away from archon");
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

        setIndicatorString(0, "move toward zombie to attack");
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
        MessageParser parser = getParserForFirstMessageOfType(roundSignals, MessageType.ENEMY);
        if (parser != null) {
            return parser.getRobotData().location;
        }

        return null;
    }
}
