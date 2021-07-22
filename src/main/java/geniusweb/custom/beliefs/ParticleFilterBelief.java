package geniusweb.custom.beliefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public class ParticleFilterBelief extends AbstractBelief {

    public static final int NUMBER_SAMPLES = 100;
    public static final Double SAMENESS_THRESHOLD = 0.1;
    private static final Double EPSILON = 0.001;

    List<AbstractPolicy> particles = new ArrayList<>();

    public ParticleFilterBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance); // particles

    }

    public ParticleFilterBelief(HashMap<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        super(opponentProbabilities, distance); // particles
    }

    @Override
    public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAgentAction, Offer lastOppAction,
            AbstractState<?> state) {
        for (AbstractPolicy abstractPolicy : this.getOpponentProbabilities().keySet()) {
            List<Bid> candidateObservations = new ArrayList<>();
            for (int i = 0; i < ParticleFilterBelief.NUMBER_SAMPLES; i++) {
                // Monte Carlo Sampling
                // This should be in a loop -- We need to try multiple actions to get an
                // understanding of whether the opponent could generate the real obs.
                this.sample(lastAgentAction, lastOppAction, state, abstractPolicy, candidateObservations);
            }
            Double weightOpponentLikelihood = candidateObservations.parallelStream()
                    .mapToDouble(obs -> this.getDistance().computeDistance(obs, realObservation.getBid()))
                    .map(val -> Math.abs(val)).sum();
            // DONE: Maybe check the size because abstractpolicies might be overridden -- Not a problem!
            this.getOpponentProbabilities().put(abstractPolicy, 1 / (weightOpponentLikelihood + EPSILON));
        }
        return new ParticleFilterBelief(this.getOpponentProbabilities(), this.getDistance());
    }

    protected void sample(Offer lastAgentAction, Offer lastOppAction, AbstractState<?> state,
            AbstractPolicy abstractPolicy, List<Bid> candidateObservations) {
        // DONE: Keep track of real observations and also supply the previous real observation
        Action chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state);
        if (chosenAction instanceof Offer) {
            candidateObservations.add(((Offer) chosenAction).getBid());
        }
    }


    // @Override
    // public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAction,
    // AbstractState<?> state) {

    // for (AbstractPolicy abstractPolicy :
    // this.getOpponentProbabilities().keySet()) {
    // List<Bid> candidateObservations = new ArrayList<>();
    // for (int i = 0; i < ParticleFilterBelief.NUMBER_SAMPLES; i++) {
    // // Monte Carlo Sampling
    // // This should be in a loop -- We need to try multiple actions to get an
    // understanding of whether the opponent could generate the real obs.
    // Action chosenAction = abstractPolicy.chooseAction(lastAction.getBid(),
    // state);
    // if (chosenAction instanceof Offer) {
    // candidateObservations.add(((Offer) chosenAction).getBid());
    // }
    // }
    // boolean hasAnyMatch = candidateObservations.parallelStream().anyMatch(obs ->
    // this.getDistance().computeDistance(obs, realObservation.getBid()) <
    // SAMENESS_THRESHOLD);
    // if (hasAnyMatch) {
    // particles.add(abstractPolicy);
    // }
    // }

    // return new ParticleFilterBelief(particles, this.getDistance());
    // }

}
