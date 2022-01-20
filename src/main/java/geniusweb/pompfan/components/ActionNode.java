package geniusweb.pompfan.components;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.StateRepresentationException;

// GENERAL NOTE: ActionWithBid exists. Should be used. Would simplify the code substantially.
public class ActionNode extends Node {

    public ActionNode(Node parent, AbstractState<?> state, Action action) {
        super(parent, state);
        this.setType(Node.NODE_TYPE.ACTION);
        this.setAction(action);
    }

    public Node receiveObservation(Double time) throws StateRepresentationException {
        int cnt = 0;
        BeliefNode child;
        Action chosenOpponentObservation;
        AbstractState<?> newUnseenState;
        
        AbstractState<?> state = this.getState();
        Action lastOpponentAction = ((BeliefNode) this.getParent()).getObservation();
        Action lastAgentAction = this.getAction();
        Bid lastOpponentBid = lastOpponentAction != null ? ((Offer) lastOpponentAction).getBid() : null;

        if (lastAgentAction instanceof Accept) {
            // This code deals with the action node being an accept. 
            // Otherwise, further exploration after will happen after an accept, 
            // which does not make sense as an acccept marks the end of a negotiation
            Accept sameAcceptAction = (Accept) lastAgentAction;

            AbstractState<?> newState = state.updateState(sameAcceptAction, time);

            child = (BeliefNode) this.getChildren().stream().filter(childNode -> childNode.getState().equals(newState))
            .findFirst().orElse(null);
            
            if(child==null && lastOpponentAction!=null){
                // take the same accept and make it a belief node
                child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newState, state.getOpponent(),
                        new Accept(lastOpponentAction.getActor(), sameAcceptAction.getBid()));
                this.addChild(child);
                return child;
            }
            // mark the end of tree branch
            return null;
        }

        do {
            Bid lastAgentBid = ((ActionWithBid) lastAgentAction).getBid();
            Action observation = state.getOpponent().chooseAction(lastAgentBid, lastOpponentBid, state);
    
            AbstractState<?> newState = state.updateState(observation, time);
    
            // choose already existent child if new state happens to be identical
            child = (BeliefNode) this.getChildren().stream().filter(childNode -> childNode.getState().equals(newState))
                    .findFirst().orElse(null);
            cnt++;
            chosenOpponentObservation = observation;
            newUnseenState = newState;
            // System.out.println("STRUGGLE BECAUSE OF OPPONENT");
            // try 100 times to generate a new observation
        } while(child != null && cnt < 100);

        if (child == null) {
            // create a new node
            child = (BeliefNode) Node.buildNode(Node.NODE_TYPE.BELIEF, this, newUnseenState, state.getOpponent(),
                    chosenOpponentObservation);
            this.addChild(child);
            return child;
        }
        // child node already exists 
        child.setIsResampled(true);
        return child;
    }

    @JsonIgnore
    public Action getAction() {
        return this.getStoredAction();
    }

    public void setAction(Action action) {
        this.setStoredAction(action);
    }

    @Override
    public String extraString() {
        return this.getState().getOpponent().getPartyId().toString();
    }

}
