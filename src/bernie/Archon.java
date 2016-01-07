package bernie;

import battlecode.common.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Archon extends Robot {
    private Set<MapLocation> archonLocations = new HashSet<MapLocation>();
    private Iterator<MapLocation> iterator = null;

    private int lastBroadcast = 0;
    private final int BROADCAST_DELAY = 60;
    private final int ENTIRE_MAP_RADIUS = 20000;

    public Archon(RobotController rc) {
        super(rc);
    }

    @Override
    public void doTurn(RobotController rc) throws GameActionException {
        int currentRound = rc.getRoundNum();
        if (currentRound < 4) {
            Signal[] signals = rc.emptySignalQueue();
            for (Signal s : signals) {
                if (s.getTeam() == enemy) {
                    archonLocations.add(s.getLocation());
                }
            }

            return;
        }

        if (currentRound > lastBroadcast + BROADCAST_DELAY) {
            if (iterator == null) {
                iterator = archonLocations.iterator();
            }

            if (iterator.hasNext()) {
                MapLocation location = iterator.next();
                rc.broadcastMessageSignal(LocationUtil.encode(location), 0, ENTIRE_MAP_RADIUS);
                lastBroadcast = currentRound;
            }
            else {
                iterator = null;
            }

            if (iterator != null
                    && !iterator.hasNext()) {
                iterator = null;
            }
        }

        if (!rc.isCoreReady()) {
            return;
        }

        RobotInfo[] nearbyZombies = senseNearbyZombies();
        RobotInfo[] nearbyEnemies = senseNearbyEnemies();
        if (Util.anyCanAttack(nearbyEnemies)
                || Util.anyCanAttack(nearbyZombies)) {
            Direction away = DirectionUtil.getDirectionAwayFrom(nearbyEnemies, nearbyZombies, rc);
            tryMove(away);
            return;
        }

        tryBuild(RobotType.SOLDIER);
    }
}
