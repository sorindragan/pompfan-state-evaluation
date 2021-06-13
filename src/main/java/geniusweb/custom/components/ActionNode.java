package geniusweb.custom.components;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;

public class ActionNode extends Node {
    private Action action;

    public ActionNode(Node parent, AbstractState<?> state, Action action) {
        super(parent, state);
        this.setType(Node.NODE_TYPE.ACTION);
        this.setAction(action);
    }

    public Node receiveObservation() throws StateRepresentationException {
        AbstractState<?> state = this.getState();
        Action observation = state.getOpponent().chooseAction();
        AbstractState<?> newState = state.updateState(observation); 
        BeliefNode child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent(), observation); 
        this.addChild(child);
        return child;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
