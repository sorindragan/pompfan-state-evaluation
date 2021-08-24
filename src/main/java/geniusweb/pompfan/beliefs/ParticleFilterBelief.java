package geniusweb.pompfan.beliefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;

public class ParticleFilterBelief extends AbstractBelief {

    public static final int NUMBER_SAMPLES = 1000; // TODO: Maybe check that out?
    private static final Double EPSILON = 0.0000000000000001; // TODO: Maybe check that out?
    private AbstractPolicy mostProbablePolicy = null;

    @JsonCreator
    public ParticleFilterBelief(@JsonProperty("opponents") List<AbstractPolicy> opponents,
            @JsonProperty("probabilities") List<Double> probabilities,
            @JsonProperty("distance") AbstractBidDistance distance) {
        super(opponents, probabilities, distance);

    }


    public ParticleFilterBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance); // particles

    }

    public ParticleFilterBelief(Map<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        super(opponentProbabilities, distance); // particles
    }

    @Override
    public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAgentAction, Offer lastOppAction,
            AbstractState<?> state) {
        Double minSum = 1000.0;
        // TODO: This is not an update
        for (AbstractPolicy abstractPolicy : this.getOpponentProbabilities().keySet()) {
            List<Bid> candidateObservations = new ArrayList<>();
            for (int i = 0; i < ParticleFilterBelief.NUMBER_SAMPLES; i++) {
                // Monte Carlo Sampling
                // This is in a loop in which we try multiple actions to get an
                // understanding of whether the opponent could generate the real obs.
                Bid sampledBid = this.sample(lastAgentAction, lastOppAction, state, abstractPolicy);

                candidateObservations.add(sampledBid);
            }
            Double weightOpponentLikelihood = candidateObservations.stream().filter(Objects::nonNull)
                    .mapToDouble(obs -> this.getDistance().computeDistance(obs, realObservation.getBid()))
                    .map(val -> Math.abs(val)).sum();
            
            if (weightOpponentLikelihood < minSum) {
                minSum = weightOpponentLikelihood;
                this.setMostProbablePolicy(abstractPolicy);
            }
            // DONE: Check the size because abstractpolicies might be overridden --
            // Not a problem!
            this.getOpponentProbabilities().put(abstractPolicy, 1 / (weightOpponentLikelihood + EPSILON));
        }
        // TODO remove
        // System.out.println(this.getMostProbablePolicy().getName());
        return returnNewBelief();
    }

    protected AbstractBelief returnNewBelief() {
        return new ParticleFilterBelief(this.getOpponentProbabilities(), this.getDistance());
    }

    protected Bid sample(Offer lastAgentAction, Offer lastOppAction, AbstractState<?> state,
            AbstractPolicy abstractPolicy) {
        // DONE: Keep track of real observations and also supply the previous real
        // observation
        Action chosenAction;
        if (lastAgentAction != null) {
            chosenAction = lastOppAction != null
                    ? abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state)
                    : abstractPolicy.chooseAction(lastAgentAction.getBid(), state);
        } else {
            // Quickfix: Random action selection if no first own best action.
            chosenAction = abstractPolicy.chooseAction();
        }

        return chosenAction instanceof Offer ? ((Offer) chosenAction).getBid() : null;
    }

    public AbstractPolicy getMostProbablePolicy() {
        return mostProbablePolicy;
    }

    public void setMostProbablePolicy(AbstractPolicy mostProbablePolicy) {
        this.mostProbablePolicy = mostProbablePolicy;
    }

}
