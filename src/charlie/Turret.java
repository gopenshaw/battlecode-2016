package charlie;

import battlecode.common.*;

public class Turret extends Robot {
    public Turret(RobotController rc) {
        super(rc);
    }

    @Override
    protected void doTurn() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();
        for (Signal s : signals) {
            if (SignalUtil.getType(s) == SignalType.PING) {
                setIndicatorString(0, SignalUtil.toString(s));
            }
        }

        if (!rc.isWeaponReady()) {
            return;
        }

        /*
        StringBuilder sb = new StringBuilder();
        MapLocation currentLocation = rc.getLocation();
        MapLocation attackLocation = null;
        int lowestHealth = 1000000;
        for (int i = 0; i < signals.length; i++) {
            if (signals[i].getTeam() == team) {
                int[] message = signals[i].getMessage();
                sb.append(message[1] + " " + Serializer.decode(message[0], currentLocation) + "; ");
                if (message[1] < lowestHealth) {
                    MapLocation target = Serializer.decode(message[0], currentLocation);
                    if (rc.canAttackLocation(target)) {
                        attackLocation = target;
                        lowestHealth = message[1];
                    }
                }
            }
        }

        setIndicatorString(1, sb.toString());

        if (attackLocation != null) {
            setIndicatorString(0, "health " + lowestHealth + " " + attackLocation);
            rc.attackLocation(attackLocation);
        }
        */
    }
}
