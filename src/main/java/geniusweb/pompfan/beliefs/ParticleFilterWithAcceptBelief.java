package geniusweb.pompfan.beliefs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;

public class ParticleFilterWithAcceptBelief extends ParticleFilterBelief {

    @JsonCreator
    public ParticleFilterWithAcceptBelief(@JsonProperty("opponents") List<AbstractPolicy> opponents,
            @JsonProperty("probabilities") List<Double> probabilities,
            @JsonProperty("distance") AbstractBidDistance distance) {
        super(opponents, probabilities, distance);

    }

    public ParticleFilterWithAcceptBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance);
    }

    public ParticleFilterWithAcceptBelief(Map<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        super(opponentProbabilities, distance); // particles
    }

    @Override
    protected Bid sample(Offer lastAgentAction, Offer lastOppAction, Offer second2LastAgentAction, 
            AbstractState<?> state, AbstractPolicy abstractPolicy) {
        // ? maybe actions that are the same should count in calculating the distance
        // example: (offer, offer), (accept, accept), (offer, accept)
        Action chosenAction;
        if (second2LastAgentAction != null) {
            // long t = System.nanoTime();
            chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(),
                    second2LastAgentAction.getBid(), state);
            // if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t) > 1000) {
                // System.out.println(abstractPolicy.getClass().getName());
                // System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t));
            // }
        } else if (lastOppAction != null) {
            chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state);
        } else if (lastAgentAction != null) {
            chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), state);
        } else {
            // Quickfix: Random action selection if no first own best action.
            chosenAction = abstractPolicy.chooseAction();
        }
        return chosenAction instanceof Offer ? ((Offer) chosenAction).getBid() : ((Accept) chosenAction).getBid();
    }

    @Override
    protected AbstractBelief returnNewBelief() {
        return new ParticleFilterWithAcceptBelief(this.getOpponentProbabilities(), this.getDistance());
    }

}
