package geniusweb.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

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

public class OwnUtilTFTAgent extends AbstractOpponent {
   
    private ExtendedUtilSpace extendedspace;
    private Bid myLastbid = null;
    private BidsWithUtility bidutils;
    private boolean DEBUG_TFT = false;

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
        this.extendedspace = new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace());
        this.bidutils = new BidsWithUtility((LinearAdditiveUtilitySpace) this.getUtilitySpace());
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
                        .max(this.extendedspace.getMax().divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(this.extendedspace.getMax());
        
        Bid selectedBid = computeNextBid(utilityGoal);
        double time = this.getProgress().get(System.currentTimeMillis());

        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
            System.out.println("TFT-Lower-Bound: " + 
                this.extendedspace.getMax().divide(new BigDecimal("2.0")).doubleValue());
        }

        myLastbid = selectedBid;

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid) 
                && time > 0.8 
                ? new Accept(this.getMe(), justLastOpponentBid)
                : new Offer(this.getMe(), selectedBid);
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {
        ImmutableList<Bid> options = this.extendedspace.getBids(utilityGoal);
        if (options.size() == BigInteger.ZERO) {
            // System.out.println("WARNING: TOLERANCE TOO LOW");

            options = this.bidutils.getBids(
	                new Interval(utilityGoal.subtract(new BigDecimal("0.1")), utilityGoal));
        }
        try {
            // this should hardly happen
            return options.get(options.size().intValue() - 1);
        } catch (Exception e) {
            System.out.println("OPP: No bid in that interval. " + utilityGoal.doubleValue());
            options = this.bidutils.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.2")), utilityGoal.add(new BigDecimal("0.1"))));
            return options.get(options.size().intValue() - 1);
        }
    }

    @Override
    public void terminate() {
        super.terminate();
    }

}
