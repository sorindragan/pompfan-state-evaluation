package geniusweb.pompfan.beliefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.distances.BothUtilityBidDistance;
import geniusweb.pompfan.distances.OppUtilityBidDistance;
import geniusweb.pompfan.distances.UtilityBidDistance;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ParticleFilterBelief extends AbstractBelief {

    public static final int NUMBER_SAMPLES = 20; // Needs to be tuned
    protected Double EPSILON = 1.0; // Also needs tuning
    private AbstractPolicy mostProbablePolicy = null;
    protected Random r = new Random();

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
        UtilitySpace agentUtility = this.getDistance().getUtilitySpace();
        // This is not an update; this overwrites the probabilities
        for (AbstractPolicy abstractPolicy : this.getOpponentProbabilities().keySet()) {
            List<Bid> candidateObservations = new ArrayList<>();
            
            // udpate utility to be the one of each opponent
            if (this.getDistance() instanceof OppUtilityBidDistance) {
                this.getDistance().setUtilitySpace(abstractPolicy.getUtilitySpace());
                // System.out.println(this.getDistance().getUtilitySpace().toString());
            }
            
            for (int i = 0; i < ParticleFilterBelief.NUMBER_SAMPLES; i++) {
                // Monte Carlo Sampling
                // This is in a loop in which we try multiple actions to get an
                // understanding of whether the opponent could generate the real obs.
                // System.out.println(state.getTime());
                AbstractState<?> newState = state;
                // the noisy time makes nonreactive agents bid diffrently
                Double noisyTime = state.getTime() + (r.nextGaussian() * 0.1);
                newState = state.setTime(Math.min(1.0, Math.max(0.0, noisyTime)));
                Bid sampledBid = this.sample(lastAgentAction, lastOppAction, newState, abstractPolicy);

                candidateObservations.add(sampledBid);
            }
            Double weightOpponentLikelihood = candidateObservations.parallelStream().filter(Objects::nonNull)
                    .mapToDouble(obs -> this.getDistance().computeDistance(obs, realObservation.getBid()))
                    .map(val -> Math.abs(val)).sum();
            
            if (this.getDistance() instanceof BothUtilityBidDistance) {
                this.getDistance().setUtilitySpace(abstractPolicy.getUtilitySpace());
                weightOpponentLikelihood += candidateObservations.parallelStream().filter(Objects::nonNull)
                        .mapToDouble(obs -> this.getDistance().computeDistance(obs, realObservation.getBid()))
                        .map(val -> Math.abs(val)).sum();
                this.getDistance().setUtilitySpace(agentUtility);
            }

            if (weightOpponentLikelihood < minSum) {
                minSum = weightOpponentLikelihood;
                this.setMostProbablePolicy(abstractPolicy);
            }
            // DONE: Check the size because abstractpolicies might be overridden --
            // Not a problem!
            this.getOpponentProbabilities().put(abstractPolicy, 1 / (weightOpponentLikelihood + EPSILON));
        }
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
