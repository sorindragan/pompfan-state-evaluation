package geniusweb.pompfan.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ConcessionUtilityEvaluator extends Last2BidsMeanUtilityEvaluator {
    protected PartyId holder;
    // protected Double lastConsession;

    public ConcessionUtilityEvaluator() {
        super();
    }

    public ConcessionUtilityEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double evaluate(HistoryState state) {
        ArrayList<Action> currState = state.getRepresentation();
        List<ActionWithBid> opponentsActions = currState.stream()
                .filter(act -> !act.getActor().equals(this.getHolder())).map(act -> (ActionWithBid) act)
                .collect(Collectors.toList());
        long count = opponentsActions.stream().count();
        if (opponentsActions.size() == 1) {
            ActionWithBid lastAction = opponentsActions.stream().skip(count - 1).findFirst().get();
            BigDecimal utility1 = this.getUtilitySpace().getUtility(lastAction.getBid());
            return utility1.doubleValue();
        }
        if (opponentsActions.size() >= 2) {
            ActionWithBid lastAction = opponentsActions.stream().skip(count - 1).findFirst().get();
            ActionWithBid preLastAction = opponentsActions.stream().skip(count - 2).findFirst().get();
            BigDecimal utility1 = this.getUtilitySpace().getUtility(lastAction.getBid());
            BigDecimal utility2 = this.getUtilitySpace().getUtility(preLastAction.getBid());
            BigDecimal difference = utility1.subtract(utility2);
            return difference.doubleValue();
        }
        return BigDecimal.ZERO.doubleValue();
    }

    public PartyId getHolder() {
        return holder;
    }

    public ConcessionUtilityEvaluator setHolder(PartyId holder) {
        this.holder = holder;
        return this;
    }

}
