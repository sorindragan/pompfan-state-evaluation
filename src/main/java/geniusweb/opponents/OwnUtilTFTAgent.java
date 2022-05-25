package geniusweb.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import geniusweb.actions.Accept;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;
import tudelft.utilities.logging.Reporter;

public class OwnUtilTFTAgent extends AbstractOpponent {
   
    private Bid myLastbid = null;
    private boolean DEBUG_TFT = false;
    private boolean DEBUG_BIDS = true;

    public OwnUtilTFTAgent(Reporter reporter) {
        super(reporter);
    }

    public OwnUtilTFTAgent() {
        super();
    }

    @Override
    protected void processOpponentAction(ActionWithBid action) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void processAgreements(Agreements agreements) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void processNonAgreements(Agreements agreements) {
        // TODO Auto-generated method stub

    }

    @Override
    protected ActionWithBid myTurn(Object param) {
        ActionWithBid action;
        ExtendedUtilSpace extendedspace = this.getExtendedspace();
        BidsWithUtility bidutils = this.getBidsWithUtility();
        List<ActionWithBid> oppHistory = this.getOpponentHistory();
        // List<ActionWithBid> ownHistory = this.getOwnHistory();
        
        if (oppHistory.size() < 3) {
            // not enough information known
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            myLastbid = bid;
            return action;
        }

        int historySize = oppHistory.size();
        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = oppHistory.get(historySize - 2).getBid();
        Bid justLastOpponentBid = oppHistory.get(historySize - 1).getBid();
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal utilityGoal = isConcession
                ? this.getUtilitySpace().getUtility(myLastbid).subtract(difference.abs())
                        .max(extendedspace.getMax().divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(extendedspace.getMax());
        
        Bid selectedBid = computeNextBid(utilityGoal, extendedspace, bidutils);
        double time = this.getProgress().get(System.currentTimeMillis());

        if (DEBUG_BIDS) {
            System.out.println(time+","+utilityGoal+","+this.getUtilitySpace().getUtility(selectedBid));
        }

        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println("O: " + time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
            System.out.println("TFT-Lower-Bound: " + 
                extendedspace.getMax().divide(new BigDecimal("2.0")).doubleValue());
        }

        myLastbid = selectedBid;

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid) 
                && time > 0.8 
                ? new Accept(this.getMe(), justLastOpponentBid)
                : new Offer(this.getMe(), selectedBid);
    }

    private Bid computeNextBid(BigDecimal utilityGoal, ExtendedUtilSpace extendedspace, BidsWithUtility bidutils) {
        if (utilityGoal.doubleValue() < extendedspace.getMin().doubleValue()) {
            utilityGoal = extendedspace.getMin();
        }

        if (utilityGoal.doubleValue() > extendedspace.getMax().doubleValue()) {
            utilityGoal = extendedspace.getMax();
        }
        ImmutableList<Bid> options = extendedspace.getBids(utilityGoal);
        return this.parseOptions(options, utilityGoal);
    }

    private Bid parseOptions(ImmutableList<Bid> options, BigDecimal targetUtil) {
        System.out.println(options.size());
        if (options.size().intValue() == 1) {
            return options.get(0);
        }

        if (options.size().intValue() == 0) {
            BigDecimal closestExistentKey = this.getSearchTree().lowerKey(targetUtil);
            closestExistentKey = closestExistentKey == null ? this.getSearchTree().higherKey(targetUtil) : closestExistentKey; 
            return this.getSearchTree().get(closestExistentKey);
        }
        Iterator<Bid> optionIterator = options.iterator();
        
        // options.forEach(this.getUtilitySpace()::getUtility);
        while (optionIterator.hasNext()) {
            Bid currBid = optionIterator.next();
            this.getSearchTree().put(this.getUtilitySpace().getUtility(currBid), currBid);
        }
        BigDecimal closestKey = this.getSearchTree().lowerKey(targetUtil);
        closestKey = closestKey == null ? this.getSearchTree().higherKey(targetUtil) : closestKey;
        Bid chosenBid = this.getSearchTree().get(closestKey);

        // System.out.println("T:" + targetUtil.setScale(6, RoundingMode.DOWN) + " C:" +
        // this.getUtilitySpace().getUtility(chosenBid) + " " + chosenBid);
        return chosenBid;
    }

    @Override
    public void terminate() {
        super.terminate();
    }

}
