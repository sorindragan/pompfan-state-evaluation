package geniusweb.custom.evaluators;

import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

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
