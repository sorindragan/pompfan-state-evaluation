package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonBackReference;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

/**
 * MeanUtilityEvaluator
 */
public class Last2BidsMeanUtilityEvaluator implements IEvalFunction<HistoryState> {
    @JsonBackReference
    private UtilitySpace utilitySpace;


    public Last2BidsMeanUtilityEvaluator() {
        super();
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }

    public Last2BidsMeanUtilityEvaluator(UtilitySpace utilitySpace) {
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
        BigDecimal mean = utility1.add(utility2).divide(new BigDecimal(2));
        return mean.doubleValue();
    }

}