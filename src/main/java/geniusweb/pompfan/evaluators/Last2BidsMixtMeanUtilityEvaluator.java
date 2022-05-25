package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonBackReference;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * Mean Utility as Evaluator
 */
public class Last2BidsMixtMeanUtilityEvaluator implements IEvalFunction<HistoryState> {
    @JsonBackReference
    private UtilitySpace utilitySpace;
    private PartyId holder;


    public Last2BidsMixtMeanUtilityEvaluator() {
        super();
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }

    public Last2BidsMixtMeanUtilityEvaluator(UtilitySpace utilitySpace) {
        super();
        this.utilitySpace = utilitySpace;
        if (this.getUtilitySpace() == null) {
            System.out.println("Something is wrong");
        }
    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        AbstractPolicy currOpp = state.getOpponent();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;
        Bid lastBid = lastAction != null ? ((ActionWithBid) lastAction).getBid() : null;
        Bid secondToLastBid = secondToLastAction != null ? ((ActionWithBid) secondToLastAction).getBid() : null;

        if (lastAction instanceof Accept) {
            return state.getUtilitySpace().getUtility(lastBid).doubleValue();
        }

        BigDecimal utility1 = BigDecimal.ZERO;
        BigDecimal utility2 = BigDecimal.ZERO;

        if (lastAction != null) {
            utility1 = lastAction.getActor().equals(currOpp.getPartyId())
                    ? currOpp.getUtilitySpace().getUtility(lastBid)
                    : this.getUtilitySpace().getUtility(lastBid);
        }
        if (secondToLastAction != null) {
            utility2 = !secondToLastAction.getActor().equals(currOpp.getPartyId())
                    ? this.getUtilitySpace().getUtility(secondToLastBid)
                    : currOpp.getUtilitySpace().getUtility(secondToLastBid);
        }
        BigDecimal mean = utility1.add(utility2).divide(new BigDecimal(2));
        return mean.doubleValue();
    }

    public PartyId getHolder() {
        return holder;
    }

    public IEvalFunction<HistoryState> setHolder(PartyId holder) {
        this.holder = holder;
        return this;
    }

}