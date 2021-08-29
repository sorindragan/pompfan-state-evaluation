package geniusweb.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.EntropyWeightedOpponentModel;
import tudelft.utilities.immutablelist.ImmutableList;

public class OppUtilTFTAgent extends GenericOpponent {
    private EntropyWeightedOpponentModel opponentModel;
    private BidsWithUtility oppBidsWithUtilities;
    private List<Bid> oppBadBids;
    private AllBidsList allPossibleBids;
    private boolean DEBUG_TFT;

    public OppUtilTFTAgent() {
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
        if (this.getOpponentHistory().size() > 5) {
            this.updateOpponentModel(oppHistory);
        }
        if (this.opponentModel == null) {
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

        BigDecimal utilityForOpponentOfLastOwnBid = this.opponentModel.getUtility(lastOwnBid);
        Interval fullRange = this.oppBidsWithUtilities.getRange();
        Interval newInterval = new Interval(this.getUtilitySpace().getUtility(lastOwnBid),
                this.getBidsWithUtility().getRange().getMax());
        ImmutableList<Bid> candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
        if (isConcession) {
            newInterval = new Interval(utilityForOpponentOfLastOwnBid,
                    utilityForOpponentOfLastOwnBid.add(difference.multiply(BigDecimal.valueOf(2))).min(BigDecimal.ONE));
            candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
            if (candiateBids.size().compareTo(BigInteger.ZERO) <= 0) {
                newInterval = new Interval(utilityForOpponentOfLastOwnBid, utilityForOpponentOfLastOwnBid
                        .add(difference.multiply(BigDecimal.valueOf(2))).min(BigDecimal.ONE));
            }
            candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
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
        ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(selectedBid, justLastOpponentBid)
                ? new Offer(this.getMe(), selectedBid)
                : new Accept(this.getMe(), justLastOpponentBid);
        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println(newInterval);
            System.out.println(result);
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility: " + this.getUtilitySpace().getUtility(result.getBid()));
        }
        return result;
    }

    private void updateOpponentModel(List<ActionWithBid> oppHistory) {
        this.getReporter().log(Level.INFO, this.getMe().toString() + ": Update OppModel!!!");
        this.opponentModel = new EntropyWeightedOpponentModel(this.getUtilitySpace().getDomain(),
                oppHistory.stream().map(a -> (Action) a).collect(Collectors.toList()));
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
        BigDecimal minUtility = this.opponentModel.getUtility(this.oppBidsWithUtilities.getExtremeBid(false));
        BigDecimal minUtilityUpperBound = minUtility.multiply(new BigDecimal("1.5")).min(BigDecimal.ONE);
        this.oppBadBids = StreamSupport
                .stream(this.oppBidsWithUtilities.getBids(new Interval(minUtility, minUtilityUpperBound)).spliterator(),
                        true)
                .collect(Collectors.toList());
    }

}
