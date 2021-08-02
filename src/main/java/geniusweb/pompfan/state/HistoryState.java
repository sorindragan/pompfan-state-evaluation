package geniusweb.pompfan.state;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Action;
import geniusweb.pompfan.evaluators.IEvalFunction;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.profile.utilityspace.UtilitySpace;

public class HistoryState extends AbstractState<ArrayList<Action>> {
    public IEvalFunction<HistoryState> evaluator;

    public HistoryState() {
        super();
    }

    public HistoryState(UtilitySpace utilitySpace, AbstractPolicy opponent,
            IEvalFunction<? extends HistoryState> evaluator) {
        super(utilitySpace, opponent);
        this.setHistory(new ArrayList<>());
        this.setEvaluator(evaluator);
    }

    public IEvalFunction<? extends HistoryState> getEvaluator() {
        return evaluator;
    }

    @SuppressWarnings("unchecked")
    public void setEvaluator(IEvalFunction<? extends HistoryState> evaluator) {
        this.evaluator = (IEvalFunction<HistoryState>) evaluator;
    }

    public ArrayList<Action> getHistory() {
        return this.getRepresentation();
    }

    public void setHistory(ArrayList<Action> history) {
        this.init(history);
    }

    @Override
    @JsonIgnore
    public String getStringRepresentation() {
        return this.getHistory().stream().map(Object::toString).collect(Collectors.joining("->"));
    }

    @Override
    public AbstractState<ArrayList<Action>> updateState(Action nextAction, Double time)
            throws StateRepresentationException {
        ArrayList<Action> representation = new ArrayList<Action>(this.getRepresentation());
        representation.add(nextAction);
        return new HistoryState(this.getUtilitySpace(), this.getOpponent(), this.getEvaluator()).init(representation).setTime(time);
    }

    @Override
    public ArrayList<Action> getCurrentState() {
        return null;
    }

    @Override
    public Double computeStateDistance(ArrayList<Action> otherState) {
        ArrayList<Action> currState = this.getRepresentation();
        return this.computeExactSame(currState, otherState);
    }

    @Override
    public Double evaluate() {
        double discountedUtility = this.evaluator.evaluate(this);
        return discountedUtility;
    }

    @Override
    public boolean equals(Object obj) {
        HistoryState secondState = (HistoryState) obj;
        return this.getHistory().equals(secondState.getHistory());
    }

}
