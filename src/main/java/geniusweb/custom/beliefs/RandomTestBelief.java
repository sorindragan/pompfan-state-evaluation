package geniusweb.custom.beliefs;

import java.util.HashMap;
import java.util.List;

import geniusweb.custom.strategies.AbstractPolicy;

public class RandomTestBelief extends AbstractBelief {

    public RandomTestBelief(List<AbstractPolicy> listOfOpponents) {
        super(listOfOpponents);
    }

    @Override
    public AbstractBelief updateBeliefs() {
        return null;
    }

}
