package geniusweb.custom.beliefs;

import java.util.List;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public class ParticleFilterWithAcceptBelief extends ParticleFilterBelief {

    public ParticleFilterWithAcceptBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        super(listOfOpponents, distance);
    }

    @Override
    protected void sample(Offer lastAgentAction, Offer lastOppAction, AbstractState<?> state,
            AbstractPolicy abstractPolicy, List<Bid> candidateObservations) {
        Action chosenAction = null;
        
        if (lastOppAction != null) {
            chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), lastOppAction.getBid(), state);
        } else {
            chosenAction = abstractPolicy.chooseAction(lastAgentAction.getBid(), state);
        }

        if (chosenAction instanceof Offer) {
            candidateObservations.add(((Offer) chosenAction).getBid());
        }
        if (chosenAction instanceof Accept) {
            candidateObservations.add(((Accept) chosenAction).getBid());
        }
    }

}
