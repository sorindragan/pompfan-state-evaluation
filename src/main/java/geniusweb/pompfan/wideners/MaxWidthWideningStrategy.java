package geniusweb.pompfan.wideners;

import java.util.HashMap;

import geniusweb.pompfan.components.ActionNode;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Node;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.progress.Progress;

public class MaxWidthWideningStrategy extends AbstractWidener {
    private Integer maxWidth;
    // AT THIS MOMENT THIS IS JUNK

    public MaxWidthWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy, HashMap<String, Object> params) {
        super(ownExplorationStrategy);
        this.maxWidth = (Integer) params.getOrDefault("maxWidth", 42);
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public MaxWidthWideningStrategy setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    @Override
    public void widen(Progress simulatedProgress, Node currRoot) throws StateRepresentationException  {
        while (currRoot.getChildren().size() == this.maxWidth) {
            // Going down the tree - Action Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            if (currRoot.getChildren().size() < this.maxWidth) {
                // Widening the Belief level

                Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                ActionNode currActionNode = (ActionNode) currRoot;
                BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);
                currRoot = receivedObservationNode;
                if (currRoot == null) {
                    return;
                } //!!!!: finish shit
                
                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value);
                return;
            } else {
                // Going down the tree - Belief Node Level
                currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            }
        }

        if (currRoot.getChildren().size() < this.maxWidth) {
            // Widening the Action level
            Double simulatedTimeOfActReceival = simulatedProgress.get(System.currentTimeMillis());
            BeliefNode currBeliefNode = (BeliefNode) currRoot;
            ActionNode receivedActionNode = (ActionNode) currBeliefNode.act(this.getOwnExplorationStrategy(),
                    simulatedTimeOfActReceival);
            ActionNode actionNode = receivedActionNode;
            Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
            BeliefNode beliefNode = (BeliefNode) actionNode.receiveObservation(simulatedTimeOfObsReceival);
            currRoot = beliefNode;
            if(currRoot == null){
                return;
            }
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }
}
