package geniusweb.custom.components;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;

public class ActionNode extends Node {

    public ActionNode(Node parent, AbstractState state) {
        super(parent, state);
        this.setType(Node.NODE_TYPE.ACTION);
    }

    public void receiveObservation() throws StateRepresentationException {
        AbstractState state = this.getState();
        Action observation = state.getOpponent().chooseAction();
        AbstractState newState = state.updateState(observation); 
        BeliefNode child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent()); 
        this.addChild(child);
    }

}
