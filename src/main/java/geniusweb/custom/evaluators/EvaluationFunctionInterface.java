package geniusweb.custom.evaluators;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;

public interface EvaluationFunctionInterface<T> {
    public Double evaluate(T state);
    // public Double evaluate(AbstractState<T> state, Bid opponenentBid);
    // public Double evaluate(AbstractState<T> state, Bid agentBid, Bid opponenentBid);
}
