package geniusweb.custom.components;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.strategies.AbstractPolicy;

/**
 * BeliefNode
 */
public class BeliefNode extends Node {

    private AbstractState state;
    private Node parent;

    public BeliefNode(Node parentNode, AbstractState state) {
        super(parentNode, state);
        this.setType(Node.NODE_TYPE.BELIEF);
    }

    public Node act(AbstractPolicy strategy) {
        AbstractState state = this.getState();
        Action agentAction = strategy.chooseAction(); // TODO: Check this one again!!!!
        ActionNode child = (ActionNode) Node.buildNode(Node.NODE_TYPE.ACTION, this, state, state.getOpponent());
        this.addChild(child);
        return this;
    }

}