package geniusweb.pompfan.wideners;

import java.util.HashMap;

import geniusweb.pompfan.components.ActionNode;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Node;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.progress.Progress;

public class ProgressiveWideningStrategy extends AbstractWidener {

    private Double k_a;
    private Double a_a;
    private Double k_b;
    private Double a_b;

    public ProgressiveWideningStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy,
            HashMap<String, Object> params) {
        super(ownExplorationStrategy);
        this.k_a = Double.parseDouble((String) params.getOrDefault("k_a", 2.0));
        this.a_a = Double.parseDouble((String) params.getOrDefault("a_a", 0.42));
        this.k_b = Double.parseDouble((String) params.getOrDefault("k_b", 1.0));
        this.a_b = Double.parseDouble((String) params.getOrDefault("a_b", 0.42));
    }

    @Override
    public void widen(Progress simulatedProgress, Long shiftSimTime, Node currRoot) throws StateRepresentationException {
        AbstractPolicy currOpp = currRoot.getState().getOpponent();
        while (currRoot.getChildren().size() == this.calcProgressiveMaxWidth(currRoot, this.k_a, this.a_a)) {
            // Going down the tree - Action Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            currRoot.getState().setOpponent(currOpp);
            
            if (currRoot.getChildren().size() < this.calcProgressiveMaxWidth(currRoot, this.k_b, this.a_b)) {
                // Widening the Belief level
                Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);

                ActionNode currActionNode = (ActionNode) currRoot;
                BeliefNode receivedObservationNode = (BeliefNode) currActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);
                currRoot = receivedObservationNode;
                
                // this if code omits the evaluation when the same accept is sampled again
                if (currRoot == null) return;
                
                currRoot.getState().setOpponent(currOpp);

                // for some reason, the same thing is sampled
                // so go deeper and and simulate 1 action node and 1 belief node further
                if (Boolean.TRUE.equals(currRoot.getIsResampled())) {
                    if (Boolean.TRUE.equals(currRoot.getIsTerminal())) {
                        return;
                    }
                    currRoot.setIsResampled(false);
                    currRoot.getParent().setIsResampled(false);
                    
                    Double simulatedTimeOfNewActReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
                    ActionNode newActionNode = (ActionNode) ((BeliefNode) currRoot)
                            .act(this.getOwnExplorationStrategy(), simulatedTimeOfNewActReceival);

                    Double simulatedTimeOfNewObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
                    BeliefNode newBeliefNode = (BeliefNode) newActionNode
                            .receiveObservation(simulatedTimeOfNewObsReceival);
                    currRoot = newBeliefNode;
                }
                
                // this if code omits the evaluation when the same accept is sampled again
                // yet again
                if (currRoot == null) return;
                
                currRoot.getState().setOpponent(currOpp);
                
                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value);
                return;
            }
            // Going down the tree - Belief Node Level
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());

            // this can happen for a small value of k_b such as 1
            // stop simulation
            if (currRoot == null) return;
            
        }
        if (currRoot.getChildren().size() < this.calcProgressiveMaxWidth(currRoot, this.k_a, this.a_a)) {
            // Widening the Action level
            Double simulatedTimeOfActReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
            BeliefNode currBeliefNode = (BeliefNode) currRoot;
            ActionNode nextActionNode = (ActionNode) currBeliefNode.act(this.getOwnExplorationStrategy(),
                    simulatedTimeOfActReceival);
            Double simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
            BeliefNode beliefNode = (BeliefNode) nextActionNode.receiveObservation(simulatedTimeOfObsReceival);
            currRoot = beliefNode;

            
            // this if code omits the evaluation when the same accept is sampled again
            if (currRoot == null) return;
            currRoot.getState().setOpponent(currOpp);

            // for some reason, the same thing is sampled (currRoot is already a children)
            // so go deeper and and simulate 1 action node and 1 belief node further
            if (Boolean.TRUE.equals(currRoot.getIsResampled())) {
                if (Boolean.TRUE.equals(currRoot.getIsTerminal())) {
                    return;
                }
                currRoot.setIsResampled(false);
                currRoot.getParent().setIsResampled(false);

                Double simulatedTimeOfNewActReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
                ActionNode newActionNode = (ActionNode) ((BeliefNode) currRoot).act(this.getOwnExplorationStrategy(),
                        simulatedTimeOfNewActReceival);

                Double simulatedTimeOfNewObsReceival = simulatedProgress.get(System.currentTimeMillis() + shiftSimTime);
                BeliefNode newBeliefNode = (BeliefNode) newActionNode.receiveObservation(simulatedTimeOfNewObsReceival);
                currRoot = newBeliefNode;
            }

            // this if code omits the evaluation when the same accept is sampled again
            // again
            if (currRoot == null) return;

            currRoot.getState().setOpponent(currOpp);
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }

    private int calcProgressiveMaxWidth(Node currRoot, Double k, Double a) {
        return Math.max(Double.valueOf(k * Math.pow(currRoot.getVisits(), a)).intValue(), 1);
    }

}
