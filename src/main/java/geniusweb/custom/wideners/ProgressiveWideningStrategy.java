package geniusweb.custom.wideners;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collector;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.components.ActionNode;
import geniusweb.custom.components.BeliefNode;
import geniusweb.custom.components.Node;
import geniusweb.custom.components.Tree;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.issuevalue.Bid;
import geniusweb.progress.Progress;

public class ProgressiveWideningStrategy extends AbstractWidener {

    private Double k_a;
    private Double a_a;
    private Double k_b;
    private Double a_b;

    public ProgressiveWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy, HashMap<String, Object> params) {
        super(ownExplorationStrategy);
        this.k_a = (Double) params.get("k_a");
        this.a_a = (Double) params.get("a_a");
        this.k_b = (Double) params.get("k_b");
        this.a_b = (Double) params.get("a_b");
    }

    @Override
    public void widen(Progress simulatedProgress, Node currRoot) throws StateRepresentationException {
        while (currRoot.getChildren().size() == this.calcProgressiveMaxWidth(currRoot, this.k_a, this.a_a)) {

            // Going down the tree - Action Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            
            if (currRoot.getChildren().size() < this.calcProgressiveMaxWidth(currRoot, this.k_b, this.a_b)) {
                // Widening the Belief level
                Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                
                ActionNode currActionNode = (ActionNode) currRoot;
                BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);

                currRoot = receivedObservationNode;
                if (currRoot == null) {
                    return;
                }
                
                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value); 
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
            ActionNode nextActionNode = (ActionNode) currBeliefNode.act(this.getOwnExplorationStrategy(),
            simulatedTimeOfActReceival);

            Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
            BeliefNode beliefNode = (BeliefNode) nextActionNode.receiveObservation(simulatedTimeOfObsReceival);
            currRoot = beliefNode;
            
            if (currRoot == null) {
                return;
            }

            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }

    private int calcProgressiveMaxWidth(Node currRoot, Double k, Double a) {
        return Double.valueOf(k * Math.pow(currRoot.getVisits(), a)).intValue() + 1;
    }

}
