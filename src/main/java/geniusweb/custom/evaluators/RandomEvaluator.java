package geniusweb.custom.evaluators;


import java.util.Random;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public class RandomEvaluator<T> implements IEvalFunction<T>{
    Random random = new Random();

    @Override
    public Double evaluate(T state) {
        return random.nextDouble();
    }

 
}
