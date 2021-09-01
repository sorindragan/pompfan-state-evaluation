package geniusweb.pompfan.evaluators;

import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ConcessionUtilityEvaluatorWithHistory extends ConcessionUtilityEvaluator{
    private Double accumulator = 0.0;
    
    public ConcessionUtilityEvaluatorWithHistory(UtilitySpace uSpace) {
        super(uSpace);
    }

    @Override
    public Double evaluate(HistoryState state) {
        accumulator += super.evaluate(state);
        return accumulator;
    }

    public Double getAccumulator() {
        return accumulator;
    }

    public void setAccumulator(Double accumulator) {
        this.accumulator = accumulator;
    }
}
