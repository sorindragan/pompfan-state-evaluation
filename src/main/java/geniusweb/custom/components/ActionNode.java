package geniusweb.custom.components;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.issuevalue.Bid;

public class ActionNode extends Node {
    private static final boolean SIM_DEBUG = false;

    public ActionNode(Node parent, AbstractState<?> state, Action action) {
        super(parent, state);
        this.setType(Node.NODE_TYPE.ACTION);
        this.setAction(action);
    }

    public Node receiveObservation(Double time) throws StateRepresentationException {
        BeliefNode child = null;
        AbstractState<?> state = this.getState();

        Action lastOpponentAction = ((BeliefNode) this.getParent()).getObservation();
        Action lastAgentAction = this.getAction();
        Bid lastOpponentBid = lastOpponentAction != null ? ((Offer) lastOpponentAction).getBid() : null;

        if (lastAgentAction instanceof Accept) {
            // ActionWithBid exists. Should use that. Simplifies the code substantially.
            // This code deals with the action node being an accept. Otherwise it will do
            // further exploration after an accept.
            
            Accept ourAcceptanceAction = (Accept) lastAgentAction;
            AbstractState<?> newState = state.updateState(ourAcceptanceAction, time);
            child = (BeliefNode) this.getChildren().stream().filter(childNode -> childNode.getState().equals(newState))
                    .findFirst().orElse(null);

            if(child==null){
                child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent(),
                        new Accept(lastOpponentAction.getActor(), ourAcceptanceAction.getBid()));
                this.addChild(child);
                return child;
            }
            return null;

        }
        Bid lastAgentBid = (lastAgentAction instanceof Offer ? ((Offer) lastAgentAction) : ((Accept) lastAgentAction))
                .getBid();
        Action observation = state.getOpponent().chooseAction(lastAgentBid, lastOpponentBid, state);

        if (SIM_DEBUG) {
            System.out.println("Counter...");
            System.out.println(observation);
        }
        AbstractState<?> newState = state.updateState(observation, time);

        child = (BeliefNode) this.getChildren().stream().filter(childNode -> childNode.getState().equals(newState))
                .findFirst().orElse(null);

        if (child == null) {
            child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent(),
                    observation);
            this.addChild(child);
            return child;
        }
        return null;
    }

    @JsonIgnore
    public Action getAction() {
        return this.getStoredAction();
    }

    public void setAction(Action action) {
        this.setStoredAction(action);
    }

}
