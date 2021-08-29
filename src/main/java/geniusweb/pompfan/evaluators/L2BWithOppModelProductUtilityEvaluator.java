package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.opponentModels.EntropyWeightedOpponentModel;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class L2BWithOppModelProductUtilityEvaluator extends Last2BidsProductUtilityEvaluator {
    protected EntropyWeightedOpponentModel oppModel;
    protected PartyId holder;

    public L2BWithOppModelProductUtilityEvaluator() {
        super();
    }

    public L2BWithOppModelProductUtilityEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double evaluate(HistoryState state) {

        ArrayList<Action> currState = state.getRepresentation();
        Action lastAction = currState.size() >= 1 ? currState.get(currState.size() - 1) : null;
        Action secondToLastAction = currState.size() >= 2 ? currState.get(currState.size() - 2) : null;

        Bid lastBid = ((ActionWithBid) lastAction).getBid();
        Bid secondToLastBid = ((ActionWithBid) secondToLastAction).getBid();

        if (lastAction instanceof Accept) {
            return state.getUtilitySpace().getUtility(lastBid).multiply(state.getUtilitySpace().getUtility(lastBid))
                    .doubleValue();
        }

        List<Action> opponentsActions = currState.stream().filter(act -> act.getActor().equals(this.getHolder()))
                .collect(Collectors.toList());
        Domain domain = state.getUtilitySpace().getDomain();
        this.setOppModel(new EntropyWeightedOpponentModel(domain, opponentsActions));

        BigDecimal utility1 = BigDecimal.ONE;
        BigDecimal utility2 = BigDecimal.ONE;
        if (lastAction != null) {
            utility1 = lastAction.getActor().equals(this.getHolder()) ? this.getUtilitySpace().getUtility(lastBid)
                    : this.getOppModel().getUtility(lastBid);
        }
        if (secondToLastAction != null) {
            utility2 = secondToLastAction.getActor().equals(this.getHolder())
                    ? this.getUtilitySpace().getUtility(secondToLastBid)
                    : this.getOppModel().getUtility(secondToLastBid);
        }

        BigDecimal prod = utility1.multiply(utility2);
        return prod.doubleValue();
    }

    public EntropyWeightedOpponentModel getOppModel() {
        return oppModel;
    }

    public void setOppModel(EntropyWeightedOpponentModel oppModel) {
        this.oppModel = oppModel;
    }

    public PartyId getHolder() {
        return holder;
    }

    public L2BWithOppModelProductUtilityEvaluator setHolder(PartyId holder) {
        this.holder = holder;
        return this;
    }
}
