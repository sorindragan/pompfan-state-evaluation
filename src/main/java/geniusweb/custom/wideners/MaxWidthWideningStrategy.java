package geniusweb.custom.wideners;

import geniusweb.custom.components.ActionNode;
import geniusweb.custom.components.BeliefNode;
import geniusweb.custom.components.Node;
import geniusweb.custom.components.Tree;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.progress.Progress;

public class MaxWidthWideningStrategy extends AbstractWidener {
    private int maxWidth;

    public MaxWidthWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy, int maxWidth) {
        super(ownExplorationStrategy);
        this.maxWidth = maxWidth;
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
                } //TODO: finish shit
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
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }
}
