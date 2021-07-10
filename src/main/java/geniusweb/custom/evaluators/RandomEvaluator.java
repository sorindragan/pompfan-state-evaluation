package geniusweb.custom.evaluators;


import java.util.Random;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public class RandomEvaluator implements EvaluationFunctionInterface{
    Random random = new Random();

    @Override
    public Double evaluate(AbstractState<?> state) {
        return random.nextDouble();
    }

    @Override
    public Double evaluate(AbstractState<?> state, Bid bid) {
        return random.nextDouble();

    }

    @Override
    public Double evaluate(AbstractState<?> state, Bid agentBid, Bid opponenentBid) {
        return random.nextDouble();
    }
    
}
