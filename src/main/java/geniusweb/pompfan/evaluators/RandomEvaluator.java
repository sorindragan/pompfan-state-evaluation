package geniusweb.pompfan.evaluators;

import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class RandomEvaluator implements IEvalFunction<Object> {
    @JsonIgnore
    Random random = null;

    public RandomEvaluator() {
        super();
        this.random = new Random();
    }

    @Override
    public Double evaluate(Object state) {
        return random.nextDouble();
    }

}
