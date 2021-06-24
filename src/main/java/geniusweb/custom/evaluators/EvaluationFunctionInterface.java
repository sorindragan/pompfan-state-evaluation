package geniusweb.custom.evaluators;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;

public interface EvaluationFunctionInterface {
    public Double evaluate(AbstractState<?> state);
    public Double evaluate(AbstractState<?> state, Bid bid);
    public Double evaluate(AbstractState<?> state, Bid agentBid, Bid opponenentBid);
}
