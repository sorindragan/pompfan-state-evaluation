package geniusweb.custom.wideners;

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
import geniusweb.progress.Progress;

public class ProgressiveWideningStrategy extends AbstractWidener {

    private Double k_a;
    private Double a_a;
    private Double k_b;
    private Double a_b;

    public ProgressiveWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy, Double k_a, Double a_a,
            Double k_b, Double a_b) {
        super(ownExplorationStrategy);
        this.k_a = k_a;
        this.a_a = a_a;
        this.k_b = k_b;
        this.a_b = a_b;
    }

    @Override
    public void widen(Progress simulatedProgress, Node currRoot) throws StateRepresentationException {
        while (currRoot.getChildren().size() == this.calcProgressiveMaxWidth(currRoot, this.k_a, this.a_a)) {

            // Going down the tree - Action Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            if (currRoot.getChildren().size() < this.calcProgressiveMaxWidth(currRoot, this.k_b, this.a_b)) {
                // Widening the Belief level

                Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                // TODO: Instead of creating new nodes it happens that we always select the
                // existing because the state we generate are already in some nodes
                // Maybe solved by adding continuous state space like time
                ActionNode currActionNode = (ActionNode) currRoot;
                BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);

                currRoot = receivedObservationNode;
                if (currActionNode.getChildren().contains(receivedObservationNode) && receivedObservationNode.getObservation() instanceof Offer) {
                    System.out.println("x");
                    simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                    ActionNode childActionNode = (ActionNode) receivedObservationNode.act(this.getOwnExplorationStrategy(), simulatedTimeOfObsReceival);
                    BeliefNode childBeliefNode = (BeliefNode) childActionNode.receiveObservation(simulatedTimeOfObsReceival);
                    currRoot = childBeliefNode;
                }
                // TODO: For non-history state evaluation, we might need the last two bids.
                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value);
                // if(currActionNode.getChildren().contains(receivedObservationNode)==false){
                    
                // }
                return;
            } else {
                // Going down the tree - Belief Node Level
                currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            }
        }

        if (currRoot.getChildren().size() < this.calcProgressiveMaxWidth(currRoot, this.k_a, this.a_a)) {
            // Widening the Action level
            Double simulatedTimeOfActReceival = simulatedProgress.get(System.currentTimeMillis());
            BeliefNode currBeliefNode = (BeliefNode) currRoot;
            ActionNode receivedActionNode = (ActionNode) currBeliefNode.act(this.getOwnExplorationStrategy(),
                    simulatedTimeOfActReceival);
            ActionNode actionNode = receivedActionNode; // What the fuck
            // System.out.println("========================================");
            Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
            BeliefNode beliefNode = (BeliefNode) actionNode.receiveObservation(simulatedTimeOfObsReceival);
            currRoot = beliefNode;
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
            // if(currActionNode.getChildren().contains(receivedObservationNode)==false){
            //     Tree.backpropagate(currRoot, value);
            // }
        }
    }

    private int calcProgressiveMaxWidth(Node currRoot, Double k, Double a) {
        return Double.valueOf(k * Math.pow(currRoot.getVisits(), a)).intValue() + 1;
    }

}
