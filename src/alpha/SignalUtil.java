package alpha;

import battlecode.common.*;

import static org.junit.Assert.assertEquals;

public class SignalUtil {
    private static RobotType[] robotTypeEncoding = {RobotType.ARCHON, RobotType.BIGZOMBIE, RobotType.FASTZOMBIE,
        RobotType.GUARD, RobotType.RANGEDZOMBIE, RobotType.SCOUT, RobotType.SOLDIER, RobotType.STANDARDZOMBIE,
        RobotType.TTM, RobotType.TURRET, RobotType.VIPER, RobotType.ZOMBIEDEN };

    private static Team[] teamEncoding = {Team.A, Team.B, Team.NEUTRAL, Team.ZOMBIE};

    public static void reportEnemy(RobotType robotType, RobotInfo robotInfo, RobotController rc) throws GameActionException {
        int encodedLocation = LocationUtil.encode(robotInfo.location);
        int dataField = buildData(robotType, robotInfo.ID, robotInfo.team);
        rc.broadcastMessageSignal(encodedLocation, dataField, 2000);
    }

    public static RobotData readSignal(Signal incoming, MapLocation validLocation) {
        int[] message = incoming.getMessage();
        if (message == null) {
            return null;
        }

        RobotData robotData = new RobotData();
        robotData.location = LocationUtil.decode(message[0], validLocation);
        robotData.robotId = getRobotId(message[1]);
        robotData.robotType = getRobotType(message[1]);
        robotData.team = getTeam(message[1]);
        return robotData;
    }

    private static int buildData(RobotType type, int robotId, Team team) {
        return robotId * 1000 + encodeRobotType(type) * 10 + encodeTeam(team);
    }

    private static int encodeTeam(Team team) {
        int encoded = 0;
        while (teamEncoding[encoded] != team) {
            encoded++;
        }

        return encoded;
    }

    private static Team getTeam(int data) {
        return decodeTeam(data % 10);
    }

    private static Team decodeTeam(int encoded) {
        return teamEncoding[encoded];
    }

    private static RobotType getRobotType(int data) {
        return decodeRobotType((data % 1000) / 10);
    }

    private static int getRobotId(int data) {
        return data / 1000;
    }

    private static int encodeRobotType(RobotType robotType) {
        int encoded = 0;
        while (robotTypeEncoding[encoded] != robotType) {
            encoded++;
        }

        return encoded;
    }

    private static RobotType decodeRobotType(int encoded) {
        return robotTypeEncoding[encoded];
    }

    /*
    @Test
    public void testSignalEncoding() {
        final RobotType type = RobotType.TTM;
        final Team team = Team.NEUTRAL;
        final int id = 782;
        int data = buildData(type, id, team);
        assertEquals(type, getRobotType(data));
        assertEquals(team, getTeam(data));
        assertEquals(id, getRobotId(data));
    }

    @Test
    public void testSignalEncoding2() {
        final RobotType type = RobotType.ZOMBIEDEN;
        final Team team = Team.ZOMBIE;
        final int id = 32000;
        int data = buildData(type, id, team);
        assertEquals(type, getRobotType(data));
        assertEquals(team, getTeam(data));
        assertEquals(id, getRobotId(data));
    }
    */
}
