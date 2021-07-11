package geniusweb.custom.components;

import geniusweb.actions.Action;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;

/**
 * BeliefNode
 */
public class BeliefNode extends Node {
    private Action observation;

    public Action getObservation() {
        return observation;
    }

    public void setObservation(Action observation) {
        this.observation = observation;
    }

    public BeliefNode(Node parentNode, AbstractState<?> state, Action observation) {
        super(parentNode, state);
        this.setType(Node.NODE_TYPE.BELIEF);
        this.setObservation(observation);
    }

    public Node act(AbstractOwnExplorationPolicy strategy, Double time) throws StateRepresentationException {
        AbstractState<?> state = this.getState();
        Action agentAction = strategy.chooseAction();
        AbstractState<?> newState = state.updateState(agentAction, time);
        // System.out.println("================== "+this.getChildren().size()+" ==================");
        // System.out.println("Parent: "+this);
        ActionNode child = (ActionNode) this.getChildren().stream()
                // .peek(e -> System.out.println("Belief: " + e))
                // .peek(e -> System.out.println("State: " + ((ActionNode) e).getAction()))
                .filter(childNode -> childNode.getState().equals(newState))
                // .peek(e -> System.out.println("Filtered value: " + e))
                .findFirst().orElse(null);

        if (child==null) {
            child = (ActionNode) Node.buildNode(Node.NODE_TYPE.ACTION, this, newState, state.getOpponent(), agentAction);
            this.addChild(child);
        }
        return child;
    }

}