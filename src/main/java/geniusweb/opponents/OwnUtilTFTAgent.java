package geniusweb.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import geniusweb.actions.Accept;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilTFTAgent extends AbstractOpponent {
    // private BidsWithUtility oppBidsWithUtilities;
    // private List<Bid> oppBadBids;
    private AllBidsList allPossibleBids;
    private boolean DEBUG_TFT;

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
        List<ActionWithBid> ownHistory = this.getOwnHistory();
        if (this.allPossibleBids == null) {
            this.allPossibleBids = new AllBidsList(this.getUtilitySpace().getDomain());
        }
        if (this.getHistory().size() <= 5) {
            // this.getReporter().log(Level.WARNING, this.getMe().toString() + ": Could not
            // get last opponent action node!!!");
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            return action;
        }

        boolean isConcession = false;
        BigDecimal difference = BigDecimal.ZERO;
        int historySize = oppHistory.size();
        Bid lastLastOpponentBid = oppHistory.get(historySize - 2).getBid();
        Bid justLastOpponentBid = oppHistory.get(historySize - 1).getBid();
        BigDecimal cntBidSpace = new BigDecimal(this.allPossibleBids.size());
        BigDecimal concessionThreshold = BigDecimal.ONE.divide(cntBidSpace, 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(2));
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        Bid lastOwnBid = ownHistory.get(ownHistory.size() - 1).getBid();
        isConcession = difference.compareTo(concessionThreshold) >= 0 ? true : false;

        BigDecimal utilityForOpponentOfLastOwnBid = this.getUtilitySpace().getUtility(lastOwnBid);
        Interval fullRange = this.getBidsWithUtility().getRange();
        Interval newInterval = new Interval(this.getUtilitySpace().getUtility(lastOwnBid),
                this.getBidsWithUtility().getRange().getMax());
        ImmutableList<Bid> candiateBids = this.getBidsWithUtility().getBids(newInterval);
        if (isConcession) {
            newInterval = new Interval(
                    utilityForOpponentOfLastOwnBid.subtract(difference.multiply(BigDecimal.valueOf(1))),
                    utilityForOpponentOfLastOwnBid);
            candiateBids = this.getBidsWithUtility().getBids(newInterval);
            if (candiateBids.size().compareTo(BigInteger.ZERO) <= 0) {
                newInterval = new Interval(
                        utilityForOpponentOfLastOwnBid.subtract(difference.multiply(BigDecimal.valueOf(2))),
                        utilityForOpponentOfLastOwnBid);
            }
            candiateBids = this.getBidsWithUtility().getBids(newInterval);
            if (candiateBids.size().compareTo(BigInteger.ZERO) <= 0) {
                newInterval = fullRange;

            }

        }
        if (candiateBids.size().compareTo(BigInteger.ZERO) == 0) {
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            return action;
        }

        int selectedIdx = this.getRandom().nextInt(candiateBids.size().intValue());
        Bid selectedBid = candiateBids.get(selectedIdx);
        ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                ? new Accept(this.getMe(), justLastOpponentBid)
                : new Offer(this.getMe(), selectedBid);
        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println(newInterval);
            System.out.println(result);
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility: " + this.getUtilitySpace().getUtility(result.getBid()));
        }
        return result;
    }
    @Override
    public void terminate() {
        super.terminate();
        this.allPossibleBids =null;
    }

}
