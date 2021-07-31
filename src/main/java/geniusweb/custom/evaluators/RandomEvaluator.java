package geniusweb.custom.evaluators;


import java.util.Random;


public class RandomEvaluator implements EvaluationFunctionInterface<Object>{
    Random random = new Random();

    @Override
    public Double evaluate(Object state) {
        return random.nextDouble();
    }
 
}
