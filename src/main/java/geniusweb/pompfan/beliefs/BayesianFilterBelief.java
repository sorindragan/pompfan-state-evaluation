package geniusweb.pompfan.beliefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;

public class BayesianFilterBelief extends ParticleFilterBelief {

    public BayesianFilterBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance);
    }
    public BayesianFilterBelief(Map<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        super(opponentProbabilities, distance);
    }

    @Override
    public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAgentAction, Offer lastOppAction,
            AbstractState<?> state) {
        Map<AbstractPolicy, Double> allJointProbabilities = this.getOpponents().parallelStream()
                .collect(Collectors.toMap(oPolicy -> oPolicy, oPolicy -> this.jointProbability(realObservation,
                        lastAgentAction, lastOppAction, state, oPolicy)));
        Double normalizingConstant = allJointProbabilities.values().parallelStream().collect(Collectors.summingDouble(x -> x));
        Map<AbstractPolicy, Double> posteriorProbabilities = this.computePosterior(allJointProbabilities, normalizingConstant);
        return new BayesianFilterBelief(posteriorProbabilities, this.getDistance());
    }

    private Map<AbstractPolicy, Double> computePosterior(Map<AbstractPolicy, Double> allJointProbabilities, Double denominator) {
        Map<AbstractPolicy, Double> posterior = allJointProbabilities.entrySet().parallelStream()
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()/denominator));
        return posterior;
    }

    protected Double prior(AbstractPolicy opp) {
        return this.getOpponentProbabilities().get(opp);
    }

    protected Double likelihood(Offer realObservation, Offer lastAgentAction, Offer lastOppAction,
            AbstractState<?> state, AbstractPolicy oPolicy) {
        Double numHits = 1.0;
        for (int i = 0; i < BayesianFilterBelief.NUMBER_SAMPLES; i++) {
            Bid sampledBid = this.sample(lastAgentAction, lastOppAction, state, oPolicy);
            numHits = this.getDistance().computeDistance(sampledBid, realObservation.getBid()) < 0.01 ? ++numHits : numHits;
        }
        return numHits / BayesianFilterBelief.NUMBER_SAMPLES;
    }

    protected Bid sample(Offer lastAgentAction, Offer lastOppAction, AbstractState<?> state,
            AbstractPolicy abstractPolicy) {
        // DONE: Keep track of real observations and also supply the previous real
        // observation
        ActionWithBid chosenAction;
        if (lastAgentAction != null) {
            chosenAction = lastOppAction != null
                    ? (ActionWithBid) abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state)
                    : (ActionWithBid) abstractPolicy.chooseAction(lastAgentAction.getBid(), state);
        } else {
            // Quickfix: Random action selection if no first own best action.
            chosenAction = (ActionWithBid) abstractPolicy.chooseAction();
        }

        return chosenAction.getBid();
    }

    protected Double jointProbability(Offer realObservation, Offer lastAgentAction, Offer lastOppAction,
            AbstractState<?> state, AbstractPolicy oPolicy) {
        // Unnormalized prior -- https://stats.stackexchange.com/a/130255
        return this.likelihood(realObservation, lastAgentAction, lastOppAction, state, oPolicy) * this.prior(oPolicy);
    }

}
