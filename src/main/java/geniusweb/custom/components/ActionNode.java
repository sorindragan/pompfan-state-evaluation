package geniusweb.custom.components;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.issuevalue.Bid;

public class ActionNode extends Node {
    private static final boolean SIM_DEBUG = false;
    private Action action;

    public ActionNode(Node parent, AbstractState<?> state, Action action) {
        super(parent, state);
        this.setType(Node.NODE_TYPE.ACTION);
        this.setAction(action);
    }

    public Node receiveObservation(Double time) throws StateRepresentationException {
        AbstractState<?> state = this.getState();
        
        Action lastOpponentAction = ((BeliefNode) this.getParent()).getObservation();
        Action lastAgentAction = this.getAction();
        Bid lastOpponentBid = lastOpponentAction != null ? ((Offer) lastOpponentAction).getBid() : null;
        Bid lastReceivedBidForOpponent = (lastAgentAction instanceof Offer ? ((Offer) lastAgentAction)  : ((Accept) lastAgentAction)).getBid();
        Action observation = state.getOpponent().chooseAction(lastReceivedBidForOpponent, lastOpponentBid, state);
        
        if (SIM_DEBUG) {
            System.out.println("Counter...");
            System.out.println(observation);
        }
        AbstractState<?> newState = state.updateState(observation, time);
      
        BeliefNode child = (BeliefNode) this.getChildren().stream()
                .filter(childNode -> childNode.getState().equals(newState))
                .findFirst().orElse(null);
        
        if (child == null) {
            child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent(),
                    observation);
            this.addChild(child);
            return child;
        }
        return null;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
