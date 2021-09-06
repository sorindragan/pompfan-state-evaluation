package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class L2BOppProdUtilEvaluator extends Last2BidsProductUtilityEvaluator {

    public L2BOppProdUtilEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    public L2BOppProdUtilEvaluator() {
    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        AbstractPolicy currOpp = state.getOpponent();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;
        Bid lastBid = lastAction instanceof Offer ? ((Offer) lastAction).getBid() : ((Accept) lastAction).getBid();
        Bid secondToLastBid = secondToLastAction instanceof Offer ? ((Offer) secondToLastAction).getBid()
                : ((Accept) secondToLastAction).getBid();
        if (lastAction instanceof Accept) {
            return state.getUtilitySpace().getUtility(lastBid).multiply(state.getUtilitySpace().getUtility(lastBid))
                    .doubleValue();
        }

        BigDecimal utility1 = BigDecimal.ONE;
        BigDecimal utility2 = BigDecimal.ONE;

        if (lastAction != null) {
            utility1 = !lastAction.getActor().equals(currOpp.getPartyId()) ? this.getUtilitySpace().getUtility(lastBid)
                    : currOpp.getUtilitySpace().getUtility(lastBid);
        }
        if (secondToLastAction != null) {
            utility2 = !secondToLastAction.getActor().equals(currOpp.getPartyId())
                    ? this.getUtilitySpace().getUtility(secondToLastBid)
                    : currOpp.getUtilitySpace().getUtility(secondToLastBid);
        }
        BigDecimal prod = utility1.multiply(utility2);
        return prod.doubleValue();
    }

}
