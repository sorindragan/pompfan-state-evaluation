package geniusweb.opponents;

import java.math.BigDecimal;
import java.util.List;

import geniusweb.actions.Accept;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import tudelft.utilities.logging.Reporter;

public class OwnUtilTFTAgent extends AbstractOpponent {
   
    private Bid myLastbid = null;
    private boolean DEBUG_TFT = false;

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
                        .max(this.maxBidWithUtil.getValue().divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(this.maxBidWithUtil.getValue());
        
        Bid selectedBid = computeNextBid(utilityGoal);
        double time = this.getProgress().get(System.currentTimeMillis());

        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println("O: " + time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
            System.out.println("TFT-Lower-Bound: " + 
                this.maxBidWithUtil.getValue().divide(new BigDecimal("2.0")).doubleValue());
        }

        myLastbid = selectedBid;

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid) 
                && time > 0.8 
                ? new Accept(this.getMe(), justLastOpponentBid)
                : new Offer(this.getMe(), selectedBid);
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {
       return this.getBidWithUtility(utilityGoal);
    }

    @Override
    public void terminate() {
        super.terminate();
    }

}
