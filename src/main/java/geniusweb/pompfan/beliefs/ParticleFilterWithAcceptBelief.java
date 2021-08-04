package geniusweb.pompfan.beliefs;

import java.util.List;
import java.util.Map;

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
    protected Bid sample(Offer lastAgentAction, Offer lastOppAction, AbstractState<?> state,
            AbstractPolicy abstractPolicy) {

        Action chosenAction;
        if (lastAgentAction != null) {
            chosenAction = lastOppAction != null
                    ? abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state)
                    : abstractPolicy.chooseAction(lastAgentAction.getBid(), state);
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
