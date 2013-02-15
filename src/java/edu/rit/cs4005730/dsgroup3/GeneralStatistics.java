package edu.rit.cs4005730.dsgroup3;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

//import edu.rit.cs4005730.dsgroup3.util.RemoteUtils;

public class GeneralStatistics implements Serializable {

    private static final long serialVersionUID = 3286190655961061721L;

    // number of seeds when the simulation started
    int initialSeedCount;

    // number of peers when the simulation started (not including seeds)
    int initialPeerCount;

    // this should be equal to seeds+peers if peers were only added once
    int totalNodeCount;

    // average download time for all completed nodes
    long meanTimeToCompletion;

    // set to true when there are no incomplete peers left
    boolean isComplete;

    public GeneralStatistics() {
        reset();
    }

    void reset() {
        reset(null, null);
    }

    void reset(List<NodeRef> peers, Map<String, NodeRef> seeds) {
        totalNodeCount = (peers == null) ? 0 : peers.size();
        initialSeedCount = (seeds == null) ? 0 : seeds.size();
        initialPeerCount = totalNodeCount - initialSeedCount;
        meanTimeToCompletion = 0;
        isComplete = false;

        // System.out.println("Stats reset: " + initialSeedCount + " seeds, " +
        // initialPeerCount + " peers");
    }

    public void updateStats(List<NodeRef> peers, Map<String, NodeRef> seeds) {
        long totalDuration = 0;
        long completedNodes = 0;
        boolean isReset = true; // gets set to false if this is not a reset
                                // update
        for (NodeRef node : peers) {
            boolean isSeed = false;
            long duration = 0;
            try {
                isSeed = node.isSeed();
                duration = node.getTransferTime();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (isSeed == true) {
                if (duration > 0) {
                    totalDuration += duration;
                    completedNodes++;
                }
                isReset &= (duration == 0); // all initial seeds have a duration
                                            // of 0
            }
            /*
             * else { // if a peer has existed for more than .5 seconds, we're
             * out of // the reset period. this is a bit of a hack isReset &=
             * (duration <= 500); complete = false; }
             */
        }

        if (isReset) {
            reset(peers, seeds);
        } else {
            meanTimeToCompletion = (completedNodes == 0) ? 0 : totalDuration
                    / completedNodes;
            totalNodeCount = peers.size();
            isComplete = (peers.size() == seeds.size());

            // System.out.println("Stats updated: average download time = " +
            // meanTimeToCompletion/1000.0 + ", node count = " + totalNodeCount
            // + " complete = " + isComplete);
            // System.out.println("average transfer speed: " +
            // RemoteUtils.formatSpeed((RemoteUtils.FILESIZE /
            // (this.meanTimeToCompletion / 1000.0))));
        }
    }

    public int getInitialPeerCount() {
        return initialPeerCount;
    }

    public int getInitialSeedCount() {
        return initialSeedCount;
    }

    public int getTotalNodeCount() {
        return totalNodeCount;
    }

    public long getMeanTimeToCompletion() {
        return meanTimeToCompletion;
    }

    public boolean getIsComplete() {
        return isComplete;
    }
}
