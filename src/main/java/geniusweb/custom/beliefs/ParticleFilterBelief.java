package geniusweb.custom.beliefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Bid;

public class ParticleFilterBelief extends AbstractBelief{

    public static final int NUMBER_SAMPLES = 100;
    public static final Double SAMENESS_THRESHOLD = 0.1;
    List<AbstractPolicy> particles = new ArrayList<>();
    private AbstractBidDistance distance;
    public ParticleFilterBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents); // particles
        this.distance = distance;
    }
    
    @Override
    public AbstractBelief updateBeliefs(Offer realObservation, Offer lastAction, AbstractState<?> state) {


        for (AbstractPolicy abstractPolicy : this.getOpponentProbabilities().keySet()) {
            List<Bid> candidateObservations = new ArrayList<>();
            for (int i = 0; i < ParticleFilterBelief.NUMBER_SAMPLES; i++) {
                Action chosenAction = abstractPolicy.chooseAction(lastAction.getBid(), state);
                if (chosenAction instanceof Offer) {
                    candidateObservations.add(((Offer) chosenAction).getBid());
                }
            }
            boolean hasAnyMatch = candidateObservations.parallelStream().anyMatch(obs -> distance.computeDistance(obs, realObservation.getBid()) < SAMENESS_THRESHOLD);
            if (hasAnyMatch) {
                particles.add(abstractPolicy);
            }
        }
        
        return new ParticleFilterBelief(particles, this.distance);
    }

}
