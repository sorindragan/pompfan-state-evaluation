package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;


public class Last2BidsProductUtilityEvaluator extends Last2BidsMeanUtilityEvaluator {

    public Last2BidsProductUtilityEvaluator() {
        super();
    }

    public Last2BidsProductUtilityEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);

    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;
        Bid lastBid = lastAction instanceof Offer ? ((Offer) lastAction).getBid() : ((Accept) lastAction).getBid();
        Bid secondToLastBid = secondToLastAction instanceof Offer ? ((Offer) secondToLastAction).getBid() : ((Accept) secondToLastAction).getBid();
        if (lastAction instanceof Accept) {
            // return state.getUtilitySpace().getUtility(lastBid).doubleValue();
            return state.getUtilitySpace().getUtility(lastBid).multiply(state.getUtilitySpace().getUtility(lastBid)).doubleValue();
        }
        BigDecimal utility1 = lastAction != null ? this.getUtilitySpace().getUtility(lastBid) : BigDecimal.ONE;
        BigDecimal utility2 = secondToLastAction != null ? this.getUtilitySpace().getUtility(secondToLastBid) : BigDecimal.ONE;
        BigDecimal prod = utility1.multiply(utility2);
        return prod.doubleValue();
    }

}