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
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.AbstractOpponentModel;
import geniusweb.pompfan.opponentModels.BetterFreqOppModel;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class OppUtilTFTAgent extends AbstractOpponent {
    private AbstractOpponentModel opponentModel = null;
    private Bid myLastbid = null;
    private BidsWithUtility oppBidsWithUtilities;
    private boolean DEBUG_TFT = false;

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
        // List<ActionWithBid> ownHistory = this.getOwnHistory();
        
        if (oppHistory.size() > 2) {
            this.updateOpponentModel(this.getHistory());
        }
        
        if (this.opponentModel == null) {
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            myLastbid = bid;
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        int historySize = oppHistory.size();
        Bid lastLastOpponentBid = oppHistory.get(historySize - 2).getBid();
        Bid justLastOpponentBid = oppHistory.get(historySize - 1).getBid();
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;
        
        BigDecimal opponentUtilityOfmyLastOwnBid = this.opponentModel.getUtility(myLastbid);
        BigDecimal utilityGoal = isConcession
                ? opponentUtilityOfmyLastOwnBid.add(difference.abs())
                        .min(BigDecimal.ONE)
                : opponentUtilityOfmyLastOwnBid.subtract(difference.abs())
                        .max(new BigDecimal("0.2"));
        
        Bid selectedBid = computeNextBid(utilityGoal);                           
        double time = this.getProgress().get(System.currentTimeMillis());
        
        if (DEBUG_TFT) {
            System.out.println("============================");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility-For-Opp: " + this.opponentModel.getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal-For-Opp: " + utilityGoal);
        }
        myLastbid = selectedBid;
        
        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                 && time > 0.8
                ? new Accept(this.getMe(), justLastOpponentBid)
                : new Offer(this.getMe(), selectedBid);
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {
        ImmutableList<Bid> options = this.oppBidsWithUtilities.getBids(
                new Interval(utilityGoal.subtract(new BigDecimal("0.2")), utilityGoal));
        if (options.size().intValue() < 1) {
            options = this.oppBidsWithUtilities.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.2")),
                            utilityGoal.add(new BigDecimal("0.2"))));
        }
        // System.out.println("TFTOpp");
        // System.out.println(utilityGoal.doubleValue());
        // System.out.println(options.size());
        try {
            return options.get(options.size().intValue() - 1);
        } catch (Exception e) {
            System.out.println("OPP: OM Faild to properly capture the utility space. " + utilityGoal.doubleValue());

            options = this.oppBidsWithUtilities.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.5")).max(BigDecimal.ZERO)
                    , utilityGoal.add(new BigDecimal("0.5")).min(BigDecimal.ONE)));
            return options.get(options.size().intValue() - 1);
        }
    }

    private void updateOpponentModel(List<ActionWithBid> history) {
        // this.getReporter().log(Level.INFO, this.getMe().toString() + ": Update OppModel!!!");
        this.opponentModel = new BetterFreqOppModel(this.getUtilitySpace().getDomain(),
                history.stream().map(a -> (Action) a).collect(Collectors.toList()), this.getMe(), this.getProgress());
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
    }

    @Override
    public void terminate() {
        super.terminate();
    }

}
