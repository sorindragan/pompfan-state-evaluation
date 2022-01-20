package geniusweb.pompfan.components;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.StateRepresentationException;

/**
 * BeliefNode
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class BeliefNode extends Node {

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
        
        AbstractState<?> state = this.getState();
        
        // in the case of the HistoryState, both lastOwnAction and lastOpponentAction should be part of the state
        // using a different state representation, that is not the case anymore
        Action lastOwnAction = this.getParent() != null ? ((ActionNode) this.getParent()).getAction() : null;
        Action lastOpponentAction = this.getObservation() instanceof Offer ? (Offer) this.getObservation() : (Accept) this.getObservation();
        Bid lastOwnBid = lastOwnAction instanceof Offer ? ((Offer) lastOwnAction).getBid() : null;
        Bid lastOpponentBid = lastOpponentAction instanceof Offer ? ((Offer) lastOpponentAction).getBid() : null;

        return this.generatePreviouslyUnseenAction(strategy, lastOpponentBid, lastOwnBid, state, time);
    }

    public Node generatePreviouslyUnseenAction(AbstractOwnExplorationPolicy strategy, Bid lastOppBid, Bid lastOwnBid, AbstractState state, Double time) throws StateRepresentationException {
        int cnt = 0;
        Action chosenAgentAction;
        AbstractState<?> newUnseenState;
        ActionNode child;
        do {
            Action agentAction = strategy.chooseAction(lastOppBid, lastOwnBid, state);
            AbstractState<?> newState = state.updateState(agentAction, time);
    
            // choose already present child if the new state happens to be identical
            child = (ActionNode) this.getChildren().parallelStream()
                    .filter(childNode -> childNode.getState().equals(newState)).findFirst().orElse(null);
            cnt++;
            chosenAgentAction = agentAction;
            newUnseenState = newState;
            // System.out.println("STRUGGLE BECAUSE OF AGENT");
            // try 100 times to generate new action
        } while(child != null && cnt < 100);

        
        if (child == null) {
            // QUESTION: Does it make sense to just always propagate the opponent? 
            // -> Too late to change the design now
            
            // create new node
            child = (ActionNode) Node.buildNode(Node.NODE_TYPE.ACTION, this, 
                    newUnseenState, state.getOpponent(),
                    chosenAgentAction);
            this.addChild(child);
            return child;
        } else {
            // change a value in the bid
            Set<String> issues = strategy.getDomain().getIssues();
            String randomIssue = issues.stream().skip(ThreadLocalRandom.current().nextInt(issues.size())).findAny().get();
            ValueSet values = strategy.getDomain().getValues(randomIssue);
            Map<String, Value> currentBidStructure = ((ActionWithBid) chosenAgentAction).getBid().getIssueValues();
            Map<String, Value> alteredBidStructure = new HashMap<>(); 
            
            for (String iss : currentBidStructure.keySet()) {
                if (iss.compareTo(randomIssue) == 0) {
                    Value currentValue = currentBidStructure.get(iss);
                    
                    int randomIdx = new Random().nextInt(values.size().intValue()-1);
                    Value newValue = values.get(randomIdx);
                    if (newValue.equals(currentValue)) {
                        newValue = values.get(values.size().intValue()-1l);
                    }
                    alteredBidStructure.put(iss, newValue);
                }
                alteredBidStructure.put(iss, currentBidStructure.get(iss));
            }
            
            Action alteredAction = new Offer(chosenAgentAction.getActor(), new Bid(alteredBidStructure));
            AbstractState<?> anotherNewState = state.updateState(alteredAction, time);

            Node lastAttemptChild = (ActionNode) this.getChildren().parallelStream()
                    .filter(childNode -> childNode.getState().equals(anotherNewState)).findFirst().orElse(null);
            if (lastAttemptChild == null) {
                lastAttemptChild = (ActionNode) Node.buildNode(Node.NODE_TYPE.ACTION, this, newUnseenState, state.getOpponent(),
                        chosenAgentAction);
                this.addChild(lastAttemptChild);
                return lastAttemptChild;
            }
            // last safenet returned an already sampled child
            lastAttemptChild.setIsResampled(true);
            return lastAttemptChild;
        }

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