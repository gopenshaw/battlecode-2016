package uber;

import battlecode.common.*;
import uber.message.MessageParser;
import uber.nav.Bug;
import uber.util.*;

public class Soldier extends Robot {
    private static final int MIN_SAFE_MOVE_ROUND = 300;

    private Signal[] roundSignals;

    private static RobotInfo[] attackableZombies;
    private static RobotInfo[] nearbyZombies;
    private static RobotInfo[] attackableEnemies;
    private static RobotInfo[] adjacentTeammates;
    private static RobotInfo[] nearbyEnemies;
    private static RobotInfo[] nearbyFriendlies;
    private static RobotInfo[] infectedEnemies;

    private static LocationCollection zombieDens = new LocationCollection(20);
    private static RobotData zombieToAttack;
    private static RobotData enemyToApproach;
    private static RobotData zombieDen;
    private static boolean[] denDestroyed = new boolean[32001];

    private static MapLocation helpLocation;
    private static final int MAX_HELP_LOCATIONS = 4;
    private static MapLocation[] helpLocations = new MapLocation[MAX_HELP_LOCATIONS];
    private static int helpLocationTurn = 0;
    private static final int IGNORE_HELP_TURNS = 3;

    private static boolean[] buggingTo = new boolean[32001];
    private BoundedQueue<RobotInfo> enemiesCanAttackMe;
    private int canAttackMe;
    private int teammatesCanAttackEnemy;
    private LocationMemory locationMemory = new LocationMemory();
    private int announceRound;
    private boolean spreadRequested;

    public Soldier(RobotController rc) {
        super(rc);
        Bug.init(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        processAllBroadcasts();
        senseRobots();
        if (suicideIfInfected()) {
            return;
        }

        shootZombies();
        shootEnemies();
        shootDen();
        microAwayFromZombiesAndInfected();
        microAwayFromEnemies();
        moveTowardZombie();
        moveTowardDen();
        moveTowardEnemy();
        moveTowardHelpLocation();
        clearRubble();
        recordZombieLocations();
        announceEnemy();
        spread();
    }

    private void spread() throws GameActionException {
        if (!rc.isCoreReady()
                || nearbyEnemies.length > 0) {
            return;
        }

        if (spreadRequested) {
            RobotInfo archon = RobotUtil.getRobotOfType(rc.senseNearbyRobots(8, team), RobotType.ARCHON);
            if (archon == null) return;
            tryMove(archon.location.directionTo(currentLocation));
        }
    }

    private boolean suicideIfInfected() throws GameActionException {
        if (!rc.isInfected()
                || nearbyEnemies.length == 0) {
            return false;
        }

        if (rc.getViperInfectedTurns() == 0
                || canAttackMe == 0) {
            return false;
        }

        RobotInfo closestRobot = RobotUtil.getClosestRobotToLocation(nearbyFriendlies, nearbyEnemies, currentLocation);
        if (closestRobot == null) {
            return false;
        }

        if (closestRobot.team == enemy) {
            return false;
        }

        if (rc.isCoreReady()) {
            setIndicatorString(2, "suicide!");
            tryMove(DirectionUtil.getDirectionToward(nearbyEnemies, currentLocation));
        }
        else {
            setIndicatorString(2, "waiting to suicide");
        }

        //--we can't do anything until we are closer to enemy
        return true;
    }

    private void announceEnemy() throws GameActionException {
        //--Announce enemy spotted every other round
        //  unless someone else already announced it
        if (nearbyEnemies.length == 0
                || helpLocationTurn == roundNumber
                || announceRound == roundNumber - 1) {
            return;
        }

        rc.broadcastSignal(senseRadius * 2);
        announceRound = roundNumber;
    }

    private void moveTowardHelpLocation() throws GameActionException {
        if (helpLocation == null
                || nearbyEnemies.length > 0
                || nearbyZombies.length > 0
                || helpLocationTurn + IGNORE_HELP_TURNS < roundNumber
                || !rc.isCoreReady()) {
            return;
        }

        setIndicatorString(2, "try move to help location");
        if (enemyTurretCount > 0) {
            trySafeMoveToward(helpLocation, enemyTurretLocations);
        }
        else {
            tryMoveToward(helpLocation);
        }
    }

    private void recordZombieLocations() {
        for (RobotInfo zombie : nearbyZombies) {
            locationMemory.saveLocation(zombie, roundNumber);
        }

        for (RobotInfo zombie : nearbyEnemies) {
            locationMemory.saveLocation(zombie, roundNumber);
        }
    }

    private void processAllBroadcasts() {
        int helpLocationCount = 0;
        enemyTurretCount = 0;
        zombieToAttack = null;
        spreadRequested = false;

        roundSignals = rc.emptySignalQueue();
        int signalCount = roundSignals.length;
        for (int i = 0; i < signalCount; i++) {
            if (roundSignals[i].getTeam() != team) {
                continue;
            }

            int[] message = roundSignals[i].getMessage();
            if (message == null) {
                //--broadcasts with no message are "help" messages
                if (helpLocationCount < MAX_HELP_LOCATIONS) {
                    helpLocations[helpLocationCount++] = roundSignals[i].getLocation();
                    helpLocationTurn = roundNumber;
                }
            }
            else {
                processBroadcastWithMessage(message);
            }
        }

        if (helpLocationCount > 0) {
            helpLocation = LocationUtil.findClosestLocation(helpLocations, helpLocationCount, currentLocation);
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
            if (MessageParser.shouldApproach(message)) {
                enemyToApproach = MessageParser.getRobotData(message[0], message[1]);
                setIndicatorString(0, "enemy to approach " + enemyToApproach.location);
            }
        }
        else if (messageType == MessageType.ENEMY_TURRET
                        && enemyTurretCount < Config.MAX_ENEMY_TURRETS) {
            enemyTurretLocations[enemyTurretCount++] = MessageParser.getLocation(message);
        }
        else if (messageType == MessageType.DESTROYED_DENS) {
            DestroyedDenData denData = MessageParser.getDestroyedDens(message[0], message[1]);
            updateDestroyedDens(denData);
        }
        else if (messageType == MessageType.SPREAD) {
            spreadRequested = true;
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
        if (!rc.isCoreReady()
                || rc.isInfected()) {
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

        setIndicatorString(0, "can attack me " + canAttackMe);
        setIndicatorString(0, "can attack enemy " + canAttackEnemy);
        if (canAttackMe + advantage > canAttackEnemy) {
            Direction directionAwayFrom = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, currentLocation);
            if (directionAwayFrom == Direction.NONE) {
                return;
            }

            Direction moveDirection = getTryMoveDirection(directionAwayFrom);
            if (moveDirection == null) {
                return;
            }

            int distanceToClosestEnemy =
                    currentLocation.distanceSquaredTo(
                            RobotUtil.getClosestRobotToLocation(nearbyEnemies, currentLocation).location);

            int distanceToClosestEnemyAfterMove =
                    currentLocation.distanceSquaredTo(
                            RobotUtil.getClosestRobotToLocation(nearbyEnemies, currentLocation.add(moveDirection)).location);

            if (distanceToClosestEnemyAfterMove < distanceToClosestEnemy) {
                return;
            }

            setIndicatorString(2, "micro away from enemy");
            tryMove(moveDirection);
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

        int enemyCount = nearbyEnemies.length;
        int allyCount = RobotUtil.countMoveReady(adjacentTeammates);
        if (allyCount < enemyCount) {
            return;
        }

        Direction towardEnemy = currentLocation.directionTo(enemyToApproach.location);
        if (RobotUtil.anyCanAttack(nearbyEnemies, currentLocation.add(towardEnemy))) {
            return;
        }

        setIndicatorString(2, "try move to enemy location");
        if (enemyTurrets.length > 0) {
            trySafeMoveToward(enemyToApproach.location, enemyTurretLocations);
        }
        else {
            if (enemyToApproach.type != RobotType.TURRET) {
                tryMoveToward(enemyToApproach.location);
            }
            else {
                trySafeMoveTowardTurret(enemyToApproach);
            }
        }
    }

    private void shootEnemies() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableEnemies.length == 0) {
            return;
        }

        RobotInfo enemyToAttack;
        if (type == RobotType.SOLDIER) {
            RobotInfo[] turrets = RobotUtil.getRobotsOfType(attackableEnemies, RobotType.TURRET);
            if (turrets.length > 0) {
                enemyToAttack = RobotUtil.getLowestHealthRobot(turrets);
            }
            else {
                enemyToAttack = RobotUtil.getLowestHealthRobot(attackableEnemies);
            }
        }
        else { // Viper
            enemyToAttack = RobotUtil.getLowestHealthNonInfectedRobot(attackableEnemies);
            if (enemyToAttack == null) {
                return;
            }
        }

        rc.attackLocation(enemyToAttack.location);
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
        if (!rc.isCoreReady()
                || nearbyEnemies.length > 0) {
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

    private void microAwayFromZombiesAndInfected() throws GameActionException {
        if (!rc.isCoreReady()
                || rc.getViperInfectedTurns() > 0) {
            return;
        }

        int x = 0;
        int y = 0;
        int count = attackableZombies.length;
        for (int i = 0; i < count; i++) {
            RobotInfo zombie = attackableZombies[i];
            MapLocation previousLocation = locationMemory.getPreviousLocation(zombie, roundNumber);
            MapLocation location = zombie.location;
            if (previousLocation != null
                    && !previousLocation.equals(location)
                    && location.distanceSquaredTo(currentLocation) < previousLocation.distanceSquaredTo(currentLocation)) {
                Direction previousDirection = previousLocation.directionTo(location);
                x += previousDirection.dx;
                y += previousDirection.dy;
            }
        }

        count = infectedEnemies.length;
        for (int i = 0; i < count; i++) {
            RobotInfo zombie = infectedEnemies[i];
            MapLocation previousLocation = locationMemory.getPreviousLocation(zombie, roundNumber);
            MapLocation location = zombie.location;
            if (previousLocation != null
                    && !previousLocation.equals(location)
                    && location.distanceSquaredTo(currentLocation) < previousLocation.distanceSquaredTo(currentLocation)) {
                Direction previousDirection = previousLocation.directionTo(location);
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
        infectedEnemies = RobotUtil.getRobotsAreInfected(nearbyEnemies);
        nearbyFriendlies = senseNearbyFriendlies();
        adjacentTeammates = rc.senseNearbyRobots(2, team);

        int maxEnemies = 6;
        enemiesCanAttackMe = RobotUtil.getEnemiesThatCanAttack(nearbyEnemies, currentLocation, maxEnemies);
        canAttackMe = enemiesCanAttackMe.getSize();
        teammatesCanAttackEnemy = RobotUtil.countCanAttack(nearbyFriendlies, enemiesCanAttackMe);
        RobotInfo den = RobotUtil.getRobotOfType(nearbyZombies, RobotType.ZOMBIEDEN);
        if (den != null) {
            zombieDens.add(den);
        }
    }

    private void shootZombies() throws GameActionException {
        if (!rc.isWeaponReady()
                || rc.getType() == RobotType.VIPER) {
            return;
        }

        RobotInfo[] nonDenZombies = RobotUtil.removeRobotsOfType(attackableZombies, RobotType.ZOMBIEDEN);
        if (nonDenZombies == null
                || nonDenZombies.length == 0) {
            return;
        }

        if (nearbyEnemies.length == 0) {
            RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(nonDenZombies);
            if (lowestHealthZombie == null) {
                return;
            }

            rc.attackLocation(lowestHealthZombie.location);
        }
        else {
            RobotInfo[] zombiesCloserToUs = RobotUtil.getRobotsCloserToUs(nonDenZombies, nearbyFriendlies, nearbyEnemies);
            RobotInfo lowestHealthZombie = RobotUtil.getLowestHealthRobot(zombiesCloserToUs);
            if (lowestHealthZombie == null) {
                return;
            }

            rc.attackLocation(lowestHealthZombie.location);
        }
    }

    private void shootDen() throws GameActionException {
        if (!rc.isWeaponReady()
                || attackableZombies.length == 0) {
            return;
        }

        RobotInfo den = RobotUtil.getRobotOfType(attackableZombies, RobotType.ZOMBIEDEN);
        if (den == null) {
            return;
        }

        rc.attackLocation(den.location);
    }

    private void moveTowardZombie() throws GameActionException {
        if (!rc.isCoreReady()
                || attackableEnemies.length > 0) {
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
