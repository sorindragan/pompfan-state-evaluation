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

}
