package geniusweb.pompfan.wideners;

import java.util.HashMap;

import geniusweb.pompfan.components.ActionNode;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Node;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.progress.Progress;

public class MaxWidthWideningStrategy extends AbstractWidener {
    private Integer maxWidth;

    public MaxWidthWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy,
            HashMap<String, Object> params) {
        super(ownExplorationStrategy);
        
        this.maxWidth = Double.valueOf((String) params.getOrDefault("maxWidth", "42.0")).intValue();
        if (this.maxWidth <= 1.0) {
            // expand a percentage of total possible bids at each level
            this.maxWidth = useMaxWidthAsPercentage(params);
        } 
    }

    private Integer useMaxWidthAsPercentage(HashMap<String, Object> params) {
        Double proportionOfMaxPossible = Double.valueOf((String) params.getOrDefault("maxWidth", "42.0"));
        Double calculatedWidth = proportionOfMaxPossible * this.getOwnExplorationStrategy().getBidspace().size().doubleValue(); 
        return calculatedWidth.intValue();
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public MaxWidthWideningStrategy setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    @Override
    public void widen(Progress simulatedProgress, Long shiftSimTime, Node currRoot) throws StateRepresentationException {
        AbstractPolicy currOpp = currRoot.getState().getOpponent();
        while (currRoot.getChildren().size() == this.maxWidth) {
            // Going down the tree - Action Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            currRoot.getState().setOpponent(currOpp);
            
            if (currRoot.getChildren().size() < this.maxWidth) {
                // Widening the Belief level

                Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
                ActionNode currActionNode = (ActionNode) currRoot;
                BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);
                currRoot = receivedObservationNode;
                
                // this if code omits the evaluation when the same accept is sampled again
                if (currRoot == null) return;
                
                currRoot.getState().setOpponent(currOpp);

                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value);
                return;
            } else {
                // Going down the tree - Belief Node Level
                currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
                // safe net
                if (currRoot == null) return;

            }
        }

        if (currRoot.getChildren().size() < this.maxWidth) {
            // Widening the Action level
            Double simulatedTimeOfActReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
            BeliefNode currBeliefNode = (BeliefNode) currRoot;
            ActionNode receivedActionNode = (ActionNode) currBeliefNode.act(this.getOwnExplorationStrategy(),
                    simulatedTimeOfActReceival);
            ActionNode actionNode = receivedActionNode;
            Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
            BeliefNode beliefNode = (BeliefNode) actionNode.receiveObservation(simulatedTimeOfObsReceival);
            currRoot = beliefNode;
            
            // same accept is sampled; skip
            if (currRoot == null) return;
            
            currRoot.getState().setOpponent(currOpp);
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }
}
