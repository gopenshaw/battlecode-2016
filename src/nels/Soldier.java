package nels;

import battlecode.common.*;
import nels.message.MessageParser;
import nels.util.BoundedQueue;
import nels.util.DirectionUtil;
import nels.util.LocationUtil;
import nels.util.RobotUtil;

public class Soldier extends Robot {
    private static final int MIN_SAFE_MOVE_ROUND = 300;

    private Signal[] roundSignals;

    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private RobotInfo[] attackableEnemies;
    private RobotInfo[] adjacentTeammates;
    private RobotInfo[] nearbyEnemies;

    BoundedQueue<Integer> zombieMemory = new BoundedQueue<Integer>(3);
    private LocationCollection zombieDens = new LocationCollection(20);
    private RobotData zombieToAttack;
    private RobotData enemyToApproach;
    private RobotData zombieDen;
    private boolean[] denDestroyed = new boolean[32001];

    private MapLocation helpLocation;
    private static final int MAX_HELP_LOCATIONS = 4;
    private MapLocation[] helpLocations = new MapLocation[MAX_HELP_LOCATIONS];
    private int helpLocationTurn = 0;
    private static final int IGNORE_HELP_TURNS = 3;

    private RobotInfo[] nearbyFriendlies;

    public Soldier(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        setBytecodeIndicator(0, "before readbroadcasts");
        processAllBroadcasts();
        setBytecodeIndicator(1, "after readbroadcasts");
        if (zombieDen == null) {
            setIndicatorString(0, "zombie den is null");
        }
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

    private void processAllBroadcasts() {
        int helpLocationCount = 0;
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
                setIndicatorString(1, "adding den " + zombie.location);
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

        int maxEnemies = 6;
        BoundedQueue<RobotInfo> enemiesCanAttackMe = RobotUtil.getEnemiesThatCanAttack(nearbyEnemies, currentLocation, maxEnemies);
        int canAttackMe = enemiesCanAttackMe.getSize();
        setIndicatorString(0, "can attack me: " + canAttackMe);
        if (canAttackMe == 0) {
            return;
        }

        int canAttackEnemy = RobotUtil.countCanAttack(nearbyFriendlies, enemiesCanAttackMe) + 1;
        int advantage = 2;
        if (canAttackMe == 1
                && canAttackEnemy == 2) {
            return;
        }

        if (canAttackMe + advantage > canAttackEnemy) {
            setIndicatorString(2, "micro away from enemies");
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
            if (enemyTurretCount > 0) {
                trySafeMoveToward(helpLocation, enemyTurretLocations);
            }
            else {
                tryMoveToward(helpLocation);
            }

            return;
        }

        if (enemyToApproach != null) {
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
    }

    private void spread() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        int nearbyArchonCount = RobotUtil.getCountOfType(nearbyFriendlies, RobotType.ARCHON);
        if (nearbyArchonCount > 0
                && adjacentTeammates.length > 3) {
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
            if (roundNumber < MIN_SAFE_MOVE_ROUND) {
                tryMoveToward(zombieDen.location);
            }
            else {
                trySafeMoveToward(zombieDen.location, enemyTurretLocations);
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
                setIndicatorString(0, "move toward zombie not gettign closer");
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
        nearbyFriendlies = senseNearbyFriendlies();
        adjacentTeammates = rc.senseNearbyRobots(2, team);
    }

    private void moveAwayFromArchon() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo archon = RobotUtil.getRobotOfType(adjacentTeammates, RobotType.ARCHON);
        if (archon != null) {
            setIndicatorString(0, "move away from archon");
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

        setIndicatorString(0, "move toward zombie");
        tryMoveToward(zombieToAttack.location);
   }
}
