package geniusweb.pompfan.beliefs;

import java.util.List;

import geniusweb.actions.Offer;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;

public class UniformBelief extends AbstractBelief {

    public UniformBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance);
    }

    @Override
    public AbstractBelief updateBeliefs(Offer newOppObservation, Offer lastRealAgentAction, Offer lastRealOppAction, Offer second2LastAgentAction,
            AbstractState<?> state) {
        return this;
    }





}
