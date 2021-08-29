package geniusweb.opponents;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.websocket.DeploymentException;

import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.inform.Agreements;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.EntropyWeightedOpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import tudelft.utilities.immutablelist.ImmutableList;

public class AntagonisticAgent extends GenericOpponent {

    private EntropyWeightedOpponentModel opponentModel;
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
            this.opponentModel = new EntropyWeightedOpponentModel(this.getUtilitySpace().getDomain(),
                    oppHistory.stream().map(a -> (Action) a).collect(Collectors.toList()));
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
        // double shouldAccept = this.getRandom().nextDouble();
        // this.getUtilitySpace().getUtility(lastOpponentsBid).compareTo(new
        // BigDecimal("0.8")) >= 0
        // this.getUtilitySpace().getUtility(antagonisticBid);
        ActionWithBid result = this.oppBadBids.contains(lastOpponentsBid) ? new Accept(this.getMe(), lastOpponentsBid)
                : new Offer(this.getMe(), antagonisticBid);
        return result;
    }

}
