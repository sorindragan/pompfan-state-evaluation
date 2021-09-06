package geniusweb.opponents;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.AHPFreqWeightedOpponentModel;
import geniusweb.pompfan.opponentModels.AbstractOpponentModel;

public class AntagonisticAgent extends GenericOpponent {

    private AbstractOpponentModel opponentModel;
    private BidsWithUtility oppBidsWithUtilities;
    private List<Bid> oppBadBids;

    public AntagonisticAgent() {
        super();
        DEBUG_OFFER = false;
        DEBUG_TIME = false;
    }

    @Override
    protected void processOpponentAction(ActionWithBid action) {

    }

    @Override
    protected void processAgreements(Agreements agreements) {

    }

    @Override
    protected void processNonAgreements(Agreements agreements) {

    }

    // @Override
    protected ActionWithBid myTurn(Object param) {
        ActionWithBid action;
        List<ActionWithBid> oppHistory = this.getOpponentHistory();

        if (this.getOpponentHistory().size() > 5) {
            this.getReporter().log(Level.INFO, this.getMe().toString() + ": Update OppModel!!!");
            this.opponentModel = new AHPFreqWeightedOpponentModel(this.getUtilitySpace().getDomain(),
                    this.getHistory().stream().map(a -> (Action) a).collect(Collectors.toList()), this.getMe());
            this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
            BigDecimal minUtility = this.opponentModel.getUtility(this.oppBidsWithUtilities.getExtremeBid(false));
            BigDecimal minUtilityUpperBound = minUtility.multiply(new BigDecimal("1.5")).min(BigDecimal.ONE);
            this.oppBadBids = StreamSupport.stream(
                    this.oppBidsWithUtilities.getBids(new Interval(minUtility, minUtilityUpperBound)).spliterator(),
                    true).collect(Collectors.toList());
        }
        if (this.opponentModel == null) {
            // this.getReporter().log(Level.WARNING, this.getMe().toString() + ": Could not
            // get last opponent action node!!!");
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            return action;
        }

        if (this.oppBadBids.size() == 0) {
            return new Offer(this.getMe(), this.oppBidsWithUtilities.getExtremeBid(false));
        }
        Bid lastOpponentsBid = oppHistory.get(oppHistory.size() - 1).getBid();
        Integer selectedIdx = this.getRandom().nextInt(this.oppBadBids.size());
        Bid antagonisticBid = this.oppBadBids.get(selectedIdx);

        ActionWithBid result = this.oppBadBids.contains(lastOpponentsBid) ? new Accept(this.getMe(), lastOpponentsBid)
                : new Offer(this.getMe(), antagonisticBid);
        return result;
    }

}
