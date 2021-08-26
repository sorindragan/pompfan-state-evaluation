package geniusweb.opponents;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.websocket.DeploymentException;

import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.inform.Agreements;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponentModels.WeightedFrequencyOpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;

public class AntagonisticAgent extends GenericOpponent<Object> {

    private WeightedFrequencyOpponentModel opponentModel;
    private BidsWithUtility oppBidsWithUtilities;

    // @SuppressWarnings("unchecked")
    // protected ActionWithBid myTurn(YourTurn myTurnInfo) {
    // ActionWithBid action;
    // List<ActionWithBid> oppHistory = this.getOpponentHistory();

    // if (this.opponentModel == null) {
    // this.getReporter().log(Level.WARNING, "Could not get last best action
    // node!!!");
    // Bid bid = this.getBidsWithUtility().getExtremeBid(true);
    // action = new Offer(this.getMe(), bid);
    // return action;
    // }
    // if ((this.getOpponentHistory().size() % 5) == 0) {
    // this.opponentModel = new
    // WeightedFrequencyOpponentModel(this.getUtilitySpace().getDomain(),
    // oppHistory.stream().map(a -> (Action) a).collect(Collectors.toList()));
    // this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
    // }

    // Bid lastOpponentsBid = oppHistory.get(oppHistory.size()).getBid();
    // Bid antagonisticBid = this.oppBidsWithUtilities.getExtremeBid(false);

    // ActionWithBid result =
    // this.getUtilitySpace().isPreferredOrEqual(lastOpponentsBid, antagonisticBid)
    // ? new Accept(this.getMe(), lastOpponentsBid)
    // : new Offer(this.getMe(), antagonisticBid);
    // return result;
    // }

    @Override
    protected void processOpponentAction(ActionWithBid action) {
        this.getHistory().add(action);
    }

    @Override
    protected void processAgreements(Agreements agreements) {

    }

    @Override
    protected void processNonAgreements(Agreements agreements) {

    }

    @Override
    protected ActionWithBid myTurn(Object param) {
        ActionWithBid action;
        List<ActionWithBid> oppHistory = this.getOpponentHistory();

        if (this.opponentModel == null) {
            this.getReporter().log(Level.WARNING, "Could not get last best action node!!!");
            Bid bid = this.getBidsWithUtility().getExtremeBid(true);
            action = new Offer(this.getMe(), bid);
            return action;
        }
        if ((this.getOpponentHistory().size() % 5) == 0) {
            this.opponentModel = new WeightedFrequencyOpponentModel(this.getUtilitySpace().getDomain(),
                    oppHistory.stream().map(a -> (Action) a).collect(Collectors.toList()));
            this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
        }

        Bid lastOpponentsBid = oppHistory.get(oppHistory.size()).getBid();
        Bid antagonisticBid = this.oppBidsWithUtilities.getExtremeBid(false);

        ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(lastOpponentsBid, antagonisticBid)
                ? new Accept(this.getMe(), lastOpponentsBid)
                : new Offer(this.getMe(), antagonisticBid);
        return result;
    }

}
