package geniusweb.custom.evaluators;


import java.util.Random;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public class RandomEvaluator implements EvaluationFunctionInterface<Object>{
    Random random = new Random();

    @Override
    public Double evaluate(Object state) {
        // TODO Auto-generated method stub
        return random.nextDouble();
    }
 
}
