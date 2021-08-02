package geniusweb.pompfan.evaluators;

import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.AbstractState;

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
