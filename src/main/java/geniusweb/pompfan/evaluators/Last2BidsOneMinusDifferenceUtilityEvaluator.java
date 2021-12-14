package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonBackReference;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * MeanUtilityEvaluator
 */
public class Last2BidsOneMinusDifferenceUtilityEvaluator implements IEvalFunction<HistoryState> {
    @JsonBackReference
    private UtilitySpace utilitySpace;
    private PartyId holder;


    public Last2BidsOneMinusDifferenceUtilityEvaluator() {
        super();
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }

    public Last2BidsOneMinusDifferenceUtilityEvaluator(UtilitySpace utilitySpace) {
        super();
        this.utilitySpace = utilitySpace;
        if (this.getUtilitySpace() == null) {
            System.out.println("Something is wrong");
        }
    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;
        Bid lastBid = lastAction instanceof Offer ? ((Offer) lastAction).getBid() : ((Accept) lastAction).getBid();
        Bid secondToLastBid = secondToLastAction instanceof Offer ? ((Offer) secondToLastAction).getBid()
                : ((Accept) secondToLastAction).getBid();
        if (lastAction instanceof Accept) {
            return state.getUtilitySpace().getUtility(lastBid).doubleValue();
        }
        BigDecimal utility1 = lastAction != null ? this.getUtilitySpace().getUtility(lastBid) : BigDecimal.ZERO;
        BigDecimal utility2 = secondToLastAction != null ? this.getUtilitySpace().getUtility(secondToLastBid)
                : BigDecimal.ZERO;
        BigDecimal difference = utility2.subtract(utility1);
        return BigDecimal.ONE.subtract(difference).doubleValue();
    }

    public PartyId getHolder() {
        return holder;
    }

    public IEvalFunction<HistoryState> setHolder(PartyId holder) {
        this.holder = holder;
        return this;
    }

}