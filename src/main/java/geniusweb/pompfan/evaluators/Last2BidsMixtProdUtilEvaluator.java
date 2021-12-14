package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class Last2BidsMixtProdUtilEvaluator extends Last2BidsProductUtilityEvaluator {
    private PartyId holder;
    
    public Last2BidsMixtProdUtilEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    public Last2BidsMixtProdUtilEvaluator() {
    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        AbstractPolicy currOpp = state.getOpponent();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;
        Bid lastBid = lastAction != null? ((ActionWithBid) lastAction).getBid() : null; 
        Bid secondToLastBid = secondToLastAction != null? ((ActionWithBid) secondToLastAction).getBid(): null; 
        
        if (lastAction instanceof Accept) {
            return state.getUtilitySpace().getUtility(lastBid).doubleValue();
        }

        BigDecimal utility1 = BigDecimal.ONE;
        BigDecimal utility2 = BigDecimal.ONE;

        if (lastAction != null) {
            utility1 = lastAction.getActor().equals(currOpp.getPartyId()) ? currOpp.getUtilitySpace().getUtility(lastBid)
                    : this.getUtilitySpace().getUtility(lastBid);
        }
        if (secondToLastAction != null) {
            utility2 = !secondToLastAction.getActor().equals(currOpp.getPartyId())
                    ? this.getUtilitySpace().getUtility(secondToLastBid)
                    : currOpp.getUtilitySpace().getUtility(secondToLastBid);
        }
        BigDecimal prod = utility1.multiply(utility2);
        return prod.doubleValue();
    }

    public PartyId getHolder() {
        return holder;
    }

    public IEvalFunction<HistoryState> setHolder(PartyId holder) {
        this.holder = holder;
        return this;
    }

}
