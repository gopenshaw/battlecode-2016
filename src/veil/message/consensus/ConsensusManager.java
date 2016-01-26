package veil.message.consensus;

import battlecode.common.*;
import veil.MessageType;
import veil.message.*;
import veil.Config;

public abstract class ConsensusManager {

    private RobotController rc;
    private Team myTeam;
    private boolean consensusReached;

    private final ConsensusRecord[] record = new ConsensusRecord[GameConstants.GAME_DEFAULT_ROUNDS + 1];
    private final int firstRound;
    private int lastProposal;
    private final RobotType type;

    protected abstract Subject getSubject();
    protected abstract int getMinimumAgeToPropose();
    protected abstract int getMinimumAgeToDeny();
    protected abstract boolean shouldPropose(int currentRound);
    protected abstract boolean shouldDeny(int currentRound);

    public ConsensusManager(RobotController rc) {
        this.rc = rc;
        this.myTeam = rc.getTeam();
        this.firstRound = rc.getRoundNum();
        this.type = rc.getType();
    }

    public void observe(Signal[] roundSignals, int currentRound) throws GameActionException {
        if (consensusReached) {
            return;
        }

        listen(roundSignals, currentRound);
        conclude(currentRound);
    }

    public void participate(Signal[] roundSignals, int currentRound) throws GameActionException {
        if (consensusReached) {
            return;
        }

        listen(roundSignals, currentRound);
        conclude(currentRound);
        if (consensusReached) {
            return;
        }

        speak(currentRound);
    }

    public boolean isConsensusReached() {
        return consensusReached;
    }

    private void listen(Signal[] roundSignals, int currentRound) {
        int signalCount = roundSignals.length;
        for (int i = 0; i < signalCount; i++) {
            Signal signal = roundSignals[i];
            if (signal.getTeam() == myTeam) {
                int[] message = signal.getMessage();
                if (message == null) continue;

                if (MessageParser.getMessageType(message[0], message[1]) == MessageType.ANNOUNCEMENT
                        && MessageParser.getSubject(message[0], message[1]) == getSubject()) {
                    if (MessageParser.getAnnouncementMode(message[0], message[1]) == AnnouncementMode.AFFIRM) {
                        consensusReached = true;
                        break;
                    }
                    else {
                        addToRecord(message, currentRound);
                    }
                }
            }
        }
    }

    private void conclude(int currentRound) {
        if (proposed(currentRound - 1)
                && !recentlyDenied(currentRound)
                && !shouldDeny(currentRound)) {
            consensusReached = true;
            if (Config.DEBUG) {
               rc.addMatchObservation(String.format("id:%d round:%d %s CONSENSUS",
                       rc.getID(), rc.getRoundNum(), getSubject()));
            }
        }
    }


    private void addToRecord(int[] message, int currentRound) {
        if (record[currentRound] == null) {
            record[currentRound] = new ConsensusRecord();
        }

        AnnouncementMode mode = MessageParser.getAnnouncementMode(message[0], message[1]);
        if (mode == AnnouncementMode.PROPOSE) {
            record[currentRound].proposed = true;
        }
        else if (mode == AnnouncementMode.DENY) {
            record[currentRound].denied = true;
        }
    }

    private void speak(int currentRound) throws GameActionException {
        if (age(currentRound) > getMinimumAgeToDeny()
                && proposed(currentRound)
                && !recentlyDenied(currentRound)
                && shouldDeny(currentRound)) {
            deny(currentRound);
            if (Config.DEBUG) {
                rc.addMatchObservation(String.format("id:%d round:%d %s DENY",
                        rc.getID(), currentRound, getSubject()));
            }
        }
        else if (age(currentRound) > getMinimumAgeToPropose()
                && iCanRetry(currentRound)
                && !proposedWithinRetryDelay(currentRound)
                && shouldPropose(currentRound)
                && type != RobotType.ARCHON) {
            propose(currentRound);
            if (Config.DEBUG) {
                rc.addMatchObservation(String.format("id:%d round:%d %s PROPOSE",
                        rc.getID(), currentRound, getSubject()));
            }
        }
    }

    private int age(int currentRound) {
        return currentRound - firstRound;
    }

    private boolean iCanRetry(int currentRound) {
        return lastProposal + getRetryDelay() < currentRound;
    }

    protected abstract int getRetryDelay();

    private boolean recentlyDenied(int currentRound) {
        return wasDenied(currentRound - 1, currentRound);
    }

    private boolean wasDenied(int earliestRound, int currentRound) {
        for (int i = earliestRound; i <= currentRound; i++) {
            if (record[i] == null) {
                continue;
            }

            if (record[i].denied) {
                return true;
            }
        }

        return false;
    }

    private boolean proposedWithinRetryDelay(int currentRound) {
        int firstRound = currentRound - getRetryDelay();
        if (firstRound < 0) {
            firstRound = 0;
        }

        return wasProposed(firstRound, currentRound);
    }

    private boolean proposed(int currentRound) {
        if (currentRound < 0) {
            return false;
        }

        if (record[currentRound] == null) {
            return false;
        }

        return record[currentRound].proposed;
    }

    private boolean wasProposed(int earliestRound, int currentRound) {
        for (int i = earliestRound; i <= currentRound; i++) {
            if (record[i] == null) {
                continue;
            }

            if (record[i].proposed) {
                return true;
            }
        }

        return false;
    }

    private void deny(int currentRound) throws GameActionException {
        recordDenial(currentRound);
        Message denial = MessageBuilder.buildAnnouncement(Subject.ZOMBIES_DEAD, AnnouncementMode.DENY);
        rc.broadcastMessageSignal(denial.getFirst(), denial.getSecond(), 80 * 80 * 2);
    }

    private void recordDenial(int currentRound) {
        if (record[currentRound] == null) {
            record[currentRound] = new ConsensusRecord();
        }

        record[currentRound].denied = true;
    }

    private void propose(int currentRound) throws GameActionException {
        recordProposal(currentRound);
        Message message = MessageBuilder.buildAnnouncement(Subject.ZOMBIES_DEAD,
                AnnouncementMode.PROPOSE);
        rc.broadcastMessageSignal(message.getFirst(), message.getSecond(), 80 * 80 * 2);
    }

    private void recordProposal(int currentRound) {
        lastProposal = currentRound;
        if (record[currentRound] == null) {
            record[currentRound] = new ConsensusRecord();
        }

        record[currentRound].proposed = true;
    }
}
