package geniusweb.custom.state;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.custom.evaluators.IEvalFunction;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class UtilityState extends AbstractState<Map<PartyId, BigDecimal>> {

    private UtilitySpace utilitySpace;
    private ArrayList<Map<PartyId, BigDecimal>> history = new ArrayList<>();
    private IEvalFunction<? extends UtilityState> evaluator;

    public UtilityState() {
        super();
    }

    public UtilityState(UtilitySpace utilitySpace, AbstractPolicy opponent,
            IEvalFunction<? extends UtilityState> evaluator) {
        super(utilitySpace, opponent);
        this.setEvaluator(evaluator);
    }

    public void setEvaluator(IEvalFunction<? extends UtilityState> evaluator) {
        this.evaluator = evaluator;
    }


    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }


    public ArrayList<Map<PartyId, BigDecimal>> getHistory() {
        return history;
    }

    public void setHistory(ArrayList<Map<PartyId, BigDecimal>> history) {
        this.history = history;
    }

    public IEvalFunction<? extends UtilityState> getEvaluator() {
        return evaluator;
    }

    @Override
    public String getStringRepresentation() {
        return this.toString();
    }

    @Override
    public AbstractState<Map<PartyId, BigDecimal>> updateState(Action nextAction, Double time)
            throws StateRepresentationException {
        Map<PartyId, BigDecimal> newRound = new HashMap<>();
        Offer offer = (Offer) nextAction;
        if (this.getRepresentation().size() < 2) {
            Map<PartyId, BigDecimal> lastRound = this.history.get(this.getRepresentation().size());
            if (lastRound.containsKey(nextAction.getActor())) {
                newRound.put(nextAction.getActor(), this.utilitySpace.getUtility(offer.getBid()));
                this.history.add(newRound);
            } else {
                lastRound.put(nextAction.getActor(), this.utilitySpace.getUtility(offer.getBid()));
            }
        }
        newRound.put(nextAction.getActor(), this.utilitySpace.getUtility(offer.getBid()));
        this.history.add(newRound);
        return this;
    }

    @Override
    public Map<PartyId, BigDecimal> getCurrentState() {
        Map<PartyId, BigDecimal> lastRound = this.history.get(this.getRepresentation().size());
        return lastRound;
    }

    @Override
    public Double computeStateDistance(Map<PartyId, BigDecimal> otherState) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Double evaluate() {
        // TODO Auto-generated method stub
        return null;
    }

}
