package jeremy;

import battlecode.common.*;
import jeremy.message.Message;
import jeremy.message.MessageBuilder;
import jeremy.util.DirectionUtil;

public class Scout extends Robot {
    private Direction exploreDirection;
    private final int LOOKAHEAD_LENGTH = 5;
    private RobotInfo[] nearbyZombies;

    public Scout(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        senseZombies();
        broadcastZombies();
        moveAwayFromZombies();
        explore();
    }

    private void broadcastZombies() throws GameActionException {
        for (RobotInfo zombie : nearbyZombies) {
            if (rc.getMessageSignalCount() >= GameConstants.MESSAGE_SIGNALS_PER_TURN) {
                break;
            }

            Message zombieMessage = MessageBuilder.buildZombieMessage(zombie, roundNumber);
            rc.broadcastMessageSignal(zombieMessage.getFirst(), zombieMessage.getSecond(), senseRadius * 2);
        }
    }

    private void senseZombies() {
        nearbyZombies = senseNearbyZombies();
    }

    private void moveAwayFromZombies() throws GameActionException {
        if (nearbyZombies.length == 0
                || !rc.isCoreReady()) {
            return;
        }

        tryMove(DirectionUtil.getDirectionAwayFrom(nearbyZombies, currentLocation));
    }

    private void explore() throws GameActionException {
        if (!rc.isCoreReady()) {
            return;
        }

        if (exploreDirection == null) {
            exploreDirection = getExploreDirection(null);
            setIndicatorString(1, "explore: " + exploreDirection);
        }

        //--check ahead
        MapLocation lookaheadLocation = currentLocation.add(exploreDirection, LOOKAHEAD_LENGTH);
        Direction newDirection = null;
        while (!rc.onTheMap(lookaheadLocation)) {
            newDirection = getExploreDirection(exploreDirection);
            setIndicatorString(1, "new: " + newDirection);
            lookaheadLocation = currentLocation.add(newDirection, LOOKAHEAD_LENGTH);
        }

        if (newDirection != null) {
            exploreDirection = newDirection;
        }

        if (rc.isCoreReady()) {
            tryMove(exploreDirection);
        }
    }


    private Direction getExploreDirection(Direction previousDirection) {
        if (previousDirection == null) {
            return getRandomDirection();
        }

        Direction newDirection = getRandomDirection();
        while (newDirection.equals(exploreDirection)
                || newDirection.opposite() == exploreDirection) {
            newDirection = getRandomDirection();
        }

        return newDirection;
    }
}
