package geniusweb.custom.beliefs;

import java.util.HashMap;
import java.util.List;

import geniusweb.actions.Offer;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;

public class RandomTestBelief extends AbstractBelief {

    public RandomTestBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance);
    }

    @Override
    public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAction, AbstractState<?> state) {
        return this;
    }



}
