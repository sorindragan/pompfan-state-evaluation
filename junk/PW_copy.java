package geniusweb.custom.junk;

import java.util.Comparator;
import java.util.stream.Collector;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.components.ActionNode;
import geniusweb.custom.components.BeliefNode;
import geniusweb.custom.components.Node;
import geniusweb.custom.components.Tree;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.custom.wideners.AbstractWidener;
import geniusweb.progress.Progress;

public class PW extends AbstractWidener {

    private int k_a;
    private double a_a;
    private Double C;

    public PW(AbstractOwnExplorationPolicy ownExplorationStrategy, Double C) {
        super(ownExplorationStrategy);
        this.C = C;
    }

    @Override
    public void widen(Progress simulatedProgress, Node currRoot) throws StateRepresentationException {

        while (simulatedProgress.isPastDeadline(System.currentTimeMillis()) == false) {

            for (Node newRoot : currRoot.getChildren()) {   
                if (newRoot.getIsTerminal()) {
                    continue;
                }             
                // Going down the tree - Action Node Level
                // currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
                if (currRoot.getChildren().size() <= (this.k_a * Math.pow(currRoot.getVisits(), this.a_a))) {
                    // Widening the Belief level
    
                    Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                    
                    // ActionNode currActionNode = (ActionNode) currRoot;
                    // BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                    //         .receiveObservation(simulatedTimeOfObsReceival);
                    // currRoot = receivedObservationNode;
                    // JUNK: For non-history state evaluation, we might need the last two bids.
                    // Action opponentAction = ((BeliefNode) currRoot).getObservation();
                    // Action agentAction = ((ActionNode) currRoot.getParent()).getAction();
                    // Double value = currRoot.getState().evaluate();
                    // Tree.backpropagate(currRoot, value);
                    return;
                } else {
                    // Going down the tree - Belief Node Level
                    currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
                }
            }
        }

        // if (simulatedProgress.isPastDeadline(System.currentTimeMillis())==false) {
        // // Widening the Action level
        // Double simulatedTimeOfActReceival =
        // simulatedProgress.get(System.currentTimeMillis());
        // BeliefNode currBeliefNode = (BeliefNode) currRoot;
        // ActionNode receivedActionNode = (ActionNode)
        // currBeliefNode.act(this.getOwnExplorationStrategy(),
        // simulatedTimeOfActReceival);
        // ActionNode actionNode = receivedActionNode; // What the fuck
        // // System.out.println("========================================");
        // Double simulatedTimeOfObsReceival =
        // simulatedProgress.get(System.currentTimeMillis());
        // BeliefNode beliefNode = (BeliefNode)
        // actionNode.receiveObservation(simulatedTimeOfObsReceival);
        // currRoot = beliefNode;
        // Double value = currRoot.getState().evaluate();
        // Tree.backpropagate(currRoot, value);
        // }

        // Action a;
        // if () {
        // a = this.chooseAction();
        // } else {
        // // a =
        // }

        // return super.chooseAction(lastReceivedBid, lastOwnBid, node);
    }

    public Action progressiveWidening(Node currRoot) {
        currRoot = currRoot.setVisits(currRoot.getVisits() + 1);
        // Integer t = currRoot.getVisits();
        // Double C = 1.0;
        // Double a = 0.5;
        // Double k = C * Math.pow(currRoot.getVisits(), a);
        Node maxNode = currRoot.getChildren().stream().max(Comparator.comparingDouble(this::score)).get();
        if (maxNode instanceof BeliefNode) {
            BeliefNode selectedNode = (BeliefNode) maxNode;
            return selectedNode.getObservation();
        }
        if (maxNode instanceof ActionNode) {
            ActionNode selectedNode = (ActionNode) maxNode;
            return selectedNode.getAction();
        }

        return null;
    }

    public Double score(Node s) {
        Double totalReward = s.getChildren().stream().mapToDouble(child -> child.getValue()).sum();
        Integer visits = s.getVisits();
        double tmpUCB = this.C * Math.sqrt(Math.log(s.getParent().getVisits()/s.getVisits()));        
        return (totalReward/visits)+tmpUCB;
    }

}
