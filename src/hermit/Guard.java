package hermit;

import battlecode.common.*;
import hermit.message.MessageParser;
import hermit.util.RobotUtil;

public class Guard extends Robot {
    private RobotInfo[] attackableZombies;
    private RobotInfo[] nearbyZombies;
    private Signal[] roundSignals;
    private RobotInfo[] adjacentFriends;

    public Guard(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        roundSignals = rc.emptySignalQueue();
        senseRobots();
        moveAwayFromDens();
        broadcastAttackableZombies();
        attackZombies();
        moveTowardZombies();
        moveToFortifyLocation();
        spreadOut();
    }

    private void moveToFortifyLocation() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        MapLocation locationToFortify = getLocationToFortify();
        if (locationToFortify != null) {
            setIndicatorString(0, "fortifying " + locationToFortify);
            tryMoveToward(locationToFortify);
        }
    }

    private MapLocation getLocationToFortify() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team
                    || s.getMessage() != null) {
                continue;
            }

            return s.getLocation();
        }

        return null;
    }

    private void broadcastAttackableZombies() throws GameActionException {
        boolean denIsSafe = getRoundsTillNextSpawn(roundNumber) > 2;

        for (RobotInfo zombie : attackableZombies) {
            if (zombie.type != RobotType.ZOMBIEDEN
                    || denIsSafe) {
                rc.broadcastSignal(senseRadius * 2);
                break;
            }
        }
    }

    private void moveAwayFromDens() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo den = RobotUtil.getRobotOfType(nearbyZombies, RobotType.ZOMBIEDEN);
        if (den == null
                || !den.location.isAdjacentTo(currentLocation)) {
            return;
        }

        if (getRoundsTillNextSpawn(roundNumber) <= 2) {
            setIndicatorString(0, "try move away from den");
            tryMove(den.location.directionTo(currentLocation));
        }
    }

    private int getRoundsTillNextSpawn(int currentRound) {
        int[] schedule = rc.getZombieSpawnSchedule().getRounds();
        for (int i = 0; i < schedule.length; i++) {
            if (schedule[i] < currentRound) {
                continue;
            }

            return schedule[i] - currentRound;
        }

        return Integer.MAX_VALUE;
    }

    private void spreadOut() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        RobotData robotRequestedSpread = getRobotRequestedSpread();
        if (robotRequestedSpread != null) {
            tryMove(robotRequestedSpread.location.directionTo(currentLocation));
        }
    }

    private RobotData getRobotRequestedSpread() {
        for (Signal s : roundSignals) {
            if (s.getTeam() != team) continue;

            int[] message = s.getMessage();
            if (message == null) continue;

            MessageParser parser = new MessageParser(message[0], message[1], currentLocation);
            if (parser.getMessageType() == MessageType.SPREAD) {
                return parser.getRobotData();
            }
        }

        return null;
    }

    private void senseRobots() {
        adjacentFriends = rc.senseNearbyRobots(2, team);
        attackableZombies = senseAttackableZombies();
        nearbyZombies = senseNearbyZombies();
    }

    private void attackZombies() throws GameActionException {
        if (!rc.isWeaponReady()) {
            return;
        }

        if (attackableZombies.length > 0) {
            rc.attackLocation(attackableZombies[0].location);
        }
    }

    private void moveTowardZombies() throws GameActionException {
        if (!rc.isCoreReady()
                || attackableZombies.length > 0
                || nearbyZombies.length == 0) {
            return;
        }

        RobotInfo zombieToMoveToward = getZombieToMoveToward(nearbyZombies);
        if (zombieToMoveToward != null) {
            setIndicatorString(0, "go to zombie " + zombieToMoveToward.location);
            tryMoveToward(zombieToMoveToward.location);
        }
    }

    private RobotInfo getZombieToMoveToward(RobotInfo[] nearbyZombies) {
        RobotInfo zombie = RobotUtil.getRobotCanAttack(nearbyZombies);
        if (zombie != null) {
            return zombie;
        }

        boolean denIsSafe = getRoundsTillNextSpawn(roundNumber) > 2;
        for (RobotInfo robot : nearbyZombies) {
            if (robot.type != RobotType.ZOMBIEDEN
                    || denIsSafe) {
                return robot;
            }
        }

        return null;
    }
}
