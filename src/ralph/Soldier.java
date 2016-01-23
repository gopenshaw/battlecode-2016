package ralph;

import battlecode.common.*;
import ralph.message.MessageParser;
import ralph.nav.Bug;
import ralph.util.*;

public class Soldier extends Robot {
    private static final int MIN_SAFE_MOVE_ROUND = 300;

    private Signal[] roundSignals;

    private static RobotInfo[] attackableZombies;
    private static RobotInfo[] nearbyZombies;
    private static RobotInfo[] attackableEnemies;
    private static RobotInfo[] adjacentTeammates;
    private static RobotInfo[] nearbyEnemies;
    private static RobotInfo[] nearbyFriendlies;

    private static LocationCollection zombieDens = new LocationCollection(20);
    private static RobotData zombieToAttack;
    private static RobotData enemyToApproach;
    private static RobotData zombieDen;
    private static boolean[] denDestroyed = new boolean[32001];

    private static final int IGNORE_HELP_TURNS = 3;

    private static boolean[] buggingTo = new boolean[32001];
    private BoundedQueue<RobotInfo> enemiesCanAttackMe;
    private int canAttackMe;
    private int teammatesCanAttackEnemy;
    private LocationMemory locationMemory = new LocationMemory();
    private RobotInfo[] adjacentEnemies;

    public Soldier(RobotController rc) {
        super(rc);
        Bug.init(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        if (roundNumber > 400) {
            enemyToApproach = new RobotData(487, new MapLocation(436, 152), 1000, RobotType.ARCHON);
        }

        processAllBroadcasts();
        senseRobots();

        RobotInfo closestRobot = RobotUtil.getClosestRobotToLocation(nearbyFriendlies, nearbyEnemies, currentLocation);
        if (rc.isInfected()
                && closestRobot != null
                && closestRobot.team == team) {
            tryMove(DirectionUtil.getDirectionToward(nearbyEnemies, currentLocation));
        }
        else {
            shootEnemiesAndZombies();
            moveTowardEnemy();
        }
    }

    private void recordZombieLocations() {
        for (RobotInfo zombie : nearbyZombies) {
            locationMemory.saveLocation(zombie, roundNumber);
        }
    }

    private void chaseArchon() throws GameActionException {
        if (nearbyEnemies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        RobotInfo archon = RobotUtil.getRobotOfType(nearbyEnemies, RobotType.ARCHON);
        if (archon == null) {
            return;
        }

        if (nearbyEnemies.length < 5
                && nearbyFriendlies.length >= nearbyEnemies.length
                && RobotUtil.countMoveReady(adjacentTeammates) > 0) {
            tryMoveToward(archon.location);
        }
        else if (nearbyFriendlies.length > nearbyEnemies.length * 2) {
            tryMoveToward(archon.location);
        }

    }

    private void processAllBroadcasts() {
        enemyTurretCount = 0;
        zombieToAttack = null;

        roundSignals = rc.emptySignalQueue();
        int signalCount = roundSignals.length;
        for (int i = 0; i < signalCount; i++) {
            if (roundSignals[i].getTeam() != team) {
                continue;
            }

            int[] message = roundSignals[i].getMessage();
            if (message == null) {
            }
            else {
                processBroadcastWithMessage(message);
            }
        }
    }

    private void processBroadcastWithMessage(int[] message) {
        MessageType messageType = MessageParser.getMessageType(message[0], message[1]);
        if (messageType == MessageType.ZOMBIE) {
            RobotData zombie = MessageParser.getRobotData(message[0], message[1]);
            if (zombie.type == RobotType.ZOMBIEDEN) {
                updateZombieDen(zombie);
            }
            else {
                zombieToAttack = zombie;
            }
        }
        else if (messageType == MessageType.ENEMY) {
            enemyToApproach = MessageParser.getRobotData(message[0], message[1]);
        }
        else if (messageType == MessageType.ENEMY_TURRET
                        && enemyTurretCount < Config.MAX_ENEMY_TURRETS) {
            enemyTurretLocations[enemyTurretCount++] = MessageParser.getLocation(message);
        }
        else if (messageType == MessageType.DESTROYED_DENS) {
            DestroyedDenData denData = MessageParser.getDestroyedDens(message[0], message[1]);
            updateDestroyedDens(denData);
        }
    }

    private void updateZombieDen(RobotData den) {
        if (denDestroyed[den.id]
                || (zombieDen != null
                && den.location == zombieDen.location)) {
            return;
        }

        //--we either add it to the queue, or replace our current destination
        //  with this one
        if (zombieDen == null) {
            zombieDens.add(den);
        }
        else if (currentLocation.distanceSquaredTo(den.location)
                < currentLocation.distanceSquaredTo(zombieDen.location)) {
            //--let broadcast den overwrite ours if it is closer
            zombieDens.add(zombieDen);
            zombieDen = den;
        }
    }

    private void microAwayFromEnemies() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (canAttackMe == 0) {
            return;
        }

        int canAttackEnemy = teammatesCanAttackEnemy + 1; //--assuming we can attack too
        int advantage = 2;
        if (canAttackMe == 1
                && canAttackEnemy == 2) {
            return;
        }

        if (canAttackMe + advantage > canAttackEnemy) {
            setIndicatorString(1, "micro away from enemies");
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation));
        }
    }

    private void moveTowardEnemy() throws GameActionException {
        if (attackableEnemies.length > 0
                || !rc.isCoreReady()) {
            return;
        }

        if (enemyToApproach == null) {
            return;
        }

        Direction towardEnemy = currentLocation.directionTo(enemyToApproach.location);
        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation.add(towardEnemy))) {
            return;
        }

        tryMoveToward(enemyToApproach.location);
    }

    private void spread() throws GameActionException {
        //--TODO write some actual spread code

        //--give archon space in early game for spawning
        if (roundNumber > 100
                || !rc.isCoreReady()) {
            return;
        }

        int nearbyArchonCount = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.ARCHON);
        if (nearbyArchonCount > 0
                && adjacentTeammates.length > 3) {
            setIndicatorString(2, "spread out");
            tryMove(DirectionUtil.getDirectionAwayFrom(adjacentTeammates, currentLocation));
        }
    }

    private void shootEnemiesAndZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        if (attackableEnemies.length == 0
                && attackableZombies.length == 0) {
            return;
        }

        RobotInfo enemyToAttack;
        if (type == RobotType.SOLDIER) {
            enemyToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
            if (enemyToAttack != null) {
                rc.attackLocation(enemyToAttack.location);
            }
        }
        else { // Viper
            int infectedCount = RobotUtil.countInfected(nearbyEnemies);
            if (2 * infectedCount >= nearbyEnemies.length
                    || nearbyEnemies.length <= 2) {
                return;
            }

            enemyToAttack = RobotUtil.getLowestHealthNonInfectedRobot(attackableEnemies);
            if (enemyToAttack != null) {
                rc.attackLocation(enemyToAttack.location);
                return;
            }
        }
    }

    private void updateDestroyedDens(DestroyedDenData denData) {
        for (int j = 0; j < denData.numberOfDens; j++) {
            int currentId = denData.denId[j];
            if (!denDestroyed[currentId]) {
                denDestroyed[currentId] = true;
                zombieDens.remove(currentId);
            }
        }
    }

    private void moveTowardDen() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nonDenZombies = RobotUtil.removeRobotsOfType(nearbyZombies, RobotType.ZOMBIEDEN);
        if (nonDenZombies != null
                && nonDenZombies.length > 0) {
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
            setIndicatorString(2, "move toward den " + zombieDen.location);
            Direction direction = currentLocation.directionTo(zombieDen.location);
            if (buggingTo[zombieDen.id]
                    || tooMuchRubble(direction)) {
                Bug.setDestination(zombieDen.location);
                buggingTo[zombieDen.id] = true;
                if (roundNumber < MIN_SAFE_MOVE_ROUND) {
                    tryMove(Bug.getDirection(currentLocation));
                }
                else {
                    trySafeMove(Bug.getDirection(currentLocation), enemyTurretLocations);
                }
            }
            else {
                if (roundNumber < MIN_SAFE_MOVE_ROUND) {
                    tryMoveToward(zombieDen.location);
                }
                else {
                    trySafeMoveToward(zombieDen.location, enemyTurretLocations);
                }
            }
        }
    }

    private boolean tooMuchRubble(Direction direction) {
        MapLocation forward = currentLocation.add(direction);
        if (rc.senseRubble(forward) < Config.TOO_MUCH_RUBBLE) {
            return false;
        }

        forward = currentLocation.add(direction.rotateLeft());
        if (rc.senseRubble(forward) < Config.TOO_MUCH_RUBBLE) {
            return false;
        }

        forward = currentLocation.add(direction.rotateRight());
        if (rc.senseRubble(forward) < Config.TOO_MUCH_RUBBLE) {
            return false;
        }

        return true;
    }

    private void microAwayFromZombies() throws GameActionException {
        if (attackableZombies.length == 0
                || !rc.isCoreReady()
                || !RobotUtil.anyCanAttack(attackableZombies)) {
            return;
        }

        int x = 0;
        int y = 0;
        for (RobotInfo zombie : attackableZombies) {
            MapLocation previousLocation = locationMemory.getPreviousLocation(zombie, roundNumber);
            if (previousLocation != null
                    && !previousLocation.equals(zombie.location)) {
                Direction previousDirection = previousLocation.directionTo(zombie.location);
                x += previousDirection.dx;
                y += previousDirection.dy;
            }
        }

        Direction zombieDirection = DirectionUtil.getDirection(x, y);
        setIndicatorString(2, "zombie direction " + zombieDirection);
        if (zombieDirection == Direction.NONE) {
            return;
        }

        if (willHitWall(currentLocation, zombieDirection)) {
            setIndicatorString(2, "move away from zombies");
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
        }
        else if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation.add(zombieDirection))) {
            setIndicatorString(2, "move away from enemies and zombies");
            tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, nearbyEnemies, currentLocation));
        }
        else {
            setIndicatorString(2, "move with zombies, " + zombieDirection);
            tryMoveForward(zombieDirection);
        }
    }

    private boolean willHitWall(MapLocation currentLocation, Direction direction) throws GameActionException {
        MapLocation next = currentLocation.add(direction, 2);
        return !rc.onTheMap(next);
    }

    private void senseRobots() {
        attackableZombies = senseAttackableZombies();
        attackableEnemies = senseAttackableEnemies();
        nearbyZombies = senseNearbyZombies();
        nearbyEnemies = senseNearbyEnemies();
        nearbyFriendlies = senseNearbyFriendlies();
        adjacentTeammates = rc.senseNearbyRobots(2, team);
        adjacentEnemies = rc.senseNearbyRobots(2, enemy);

        int maxEnemies = 6;
        enemiesCanAttackMe = RobotUtil.getEnemiesThatCanAttack(nearbyEnemies, currentLocation, maxEnemies);
        canAttackMe = enemiesCanAttackMe.getSize();
        teammatesCanAttackEnemy = RobotUtil.countCanAttack(nearbyFriendlies, enemiesCanAttackMe);
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
        if (!rc.isWeaponReady()) {
            return;
        }

        RobotInfo[] nonDenZombies = RobotUtil.removeRobotsOfType(attackableZombies, RobotType.ZOMBIEDEN);
        if (nonDenZombies == null
                || nonDenZombies.length == 0) {
            return;
        }

        RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(attackableZombies);
        if (lowestHealthZombie == null) {
            return;
        }

        rc.attackLocation(lowestHealthZombie.location);
    }

    private void shootDen() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableZombies.length == 0) {
            return;
        }

        RobotInfo den = RobotUtil.getRobotOfType(attackableZombies, RobotType.ZOMBIEDEN);
        rc.attackLocation(den.location);
    }

    private void moveTowardZombie() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (zombieToAttack == null) {
            return;
        }

        if (rc.canSenseRobot(zombieToAttack.id)) {
            return;
        }

        setIndicatorString(2, "move toward broadcast zombie");
        tryMoveToward(zombieToAttack.location);
   }
}
