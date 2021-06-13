package geniusweb.custom.evaluators;


import java.util.Random;

import geniusweb.custom.state.AbstractState;

public class RandomEvaluator implements AbstractEvaluationFunction{
    Random random = new Random();

    @Override
    public Double evaluate(AbstractState<?> state) {
        return random.nextDouble();
    }
    
}
