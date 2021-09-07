package geniusweb.pompfan.components;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.StateRepresentationException;

/**
 * BeliefNode
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class BeliefNode extends Node {
    private static final boolean SIM_DEBUG = false;

    public BeliefNode() {
        super();
    }

    @JsonIgnore
    public Action getObservation() {
        return this.getStoredAction();
    }

    public void setObservation(Action observation) {
        this.setStoredAction(observation);
    }

    // @JsonCreator
    public BeliefNode(Node parentNode, AbstractState<?> state, Action observation) {
        super(parentNode, state);
        this.setType(Node.NODE_TYPE.BELIEF);
        this.setObservation(observation);
    }

    public Node act(AbstractOwnExplorationPolicy strategy, Double time) throws StateRepresentationException {
        if (time > 0.5) {
            System.out.println("stuff");
        }
        
        AbstractState<?> state = this.getState();
        
        // ? both lastOwnAction and lastOpponentAction should be part of the state
        Action lastOwnAction = this.getParent() != null ? ((ActionNode) this.getParent()).getAction() : null;
        Action lastOpponentAction = this.getObservation() instanceof Offer ? (Offer) this.getObservation() : (Accept) this.getObservation();
        Bid lastOwnBid = lastOwnAction instanceof Offer ? ((Offer) lastOwnAction).getBid() : null;
        Bid lastOpponentBid = lastOpponentAction instanceof Offer ? ((Offer) lastOpponentAction).getBid() : null;

        // WE CAN DO THIS. WE ONLY HAVE TO WHILE THE SHIT OUT OF THE ACTION BRUTE-FORCE AND IF THE CHILD IS STILL ALREADY KNOWN WE JUST CHANGE THE BID A LITTLE
        Action agentAction = strategy.chooseAction(lastOpponentBid, lastOwnBid, state);
        
        if (SIM_DEBUG) {
            System.out.println("Choose...");
            System.out.println(agentAction);
        }

        AbstractState<?> newState = state.updateState(agentAction, time);
        
        // choose already present child if the new state happens to be identical
        ActionNode child = (ActionNode) this.getChildren().stream()
               .filter(childNode -> childNode.getState().equals(newState))
               .findFirst().orElse(null);

        if (child == null) {
            // create new node
            // QUESTION: Does it make sense to just always propagate the opponent?
            child = (ActionNode) Node.buildNode(Node.NODE_TYPE.ACTION, this, newState, state.getOpponent(),
                    agentAction);
            this.addChild(child);
            return child;
        }
        child.setIsResampled(true);
        return child;
    }

    @Override
    public String extraString() {
        if (this.getState() == null) {
            return "[no state]";
        }
        if (this.getState().getOpponent() == null) {
            return "[no state & no opponent]";
        }
        return this.getState().getOpponent().getPartyId().toString();
    }

}