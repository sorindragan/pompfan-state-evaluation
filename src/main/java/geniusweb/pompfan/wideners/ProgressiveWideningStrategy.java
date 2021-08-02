package geniusweb.pompfan.wideners;

import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collector;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.components.ActionNode;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Node;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
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
        this.k_a = (Double) params.getOrDefault("k_a", 42.0);
        this.a_a = (Double) params.getOrDefault("a_a", 0.42);
        this.k_b = (Double) params.getOrDefault("k_b", 42.0);
        this.a_b = (Double) params.getOrDefault("a_b", 0.42);
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
                
                // this if code omits the evaluation when the same accept is sampled again
                if (currRoot == null) {
                    return;
                }

                // for some reason, the same shit is sampled (currRoot is already a children)
                // so go deeper and and simulate 1 action node and 1 belief node further
                // this was not observed at this level tho
                if (currActionNode.getChildren().contains(currRoot)) {
                    if (Boolean.TRUE.equals(currRoot.getIsTerminal())) {
                        return;
                    }
                    Double simulatedTimeOfNewActReceival = simulatedProgress.get(System.currentTimeMillis());
                    ActionNode newActionNode = (ActionNode) ((BeliefNode) currRoot).act(this.getOwnExplorationStrategy(),
                            simulatedTimeOfNewActReceival);

                    Double simulatedTimeOfNewObsReceival = simulatedProgress.get(System.currentTimeMillis());
                    BeliefNode newBeliefNode = (BeliefNode) newActionNode
                            .receiveObservation(simulatedTimeOfNewObsReceival);
                    currRoot = newBeliefNode;
                }

                // this if code omits the evaluation when the same accept is sampled again
                // yet again
                if (currRoot == null) {
                    return;
                }

                Double value = currRoot.getState().evaluate();
                Tree.backpropagate(currRoot, value);
                return;
            } else {
                ActionNode oldRoot = (ActionNode) currRoot;
                // Going down the tree - Belief Node Level
                currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
                // Safenet in case of terminal children only
                // Add a new action node
                if (currRoot == null) {
                    Double simulatedTimeOfNewNode = simulatedProgress.get(System.currentTimeMillis());
                    BeliefNode newChild = (BeliefNode) oldRoot
                            .receiveObservation(simulatedTimeOfNewNode);
                    currRoot = newChild;
                    Double value = currRoot.getState().evaluate();
                    Tree.backpropagate(currRoot, value);
                    return;
                }
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

            // this if code omits the evaluation when the same accept is sampled again
            if (currRoot == null) {
                return;
            }

            // for some reason, the same shit is sampled (currRoot is already a children)
            // so go deeper and and simulate 1 action node and 1 belief node further
            if (nextActionNode.getChildren().contains(currRoot)) {
                if (Boolean.TRUE.equals(currRoot.getIsTerminal())) {
                    return;
                }
                Double simulatedTimeOfNewActReceival = simulatedProgress.get(System.currentTimeMillis());
                ActionNode newActionNode = (ActionNode) ((BeliefNode) currRoot).act(this.getOwnExplorationStrategy(),
                        simulatedTimeOfNewActReceival);
                Double simulatedTimeOfNewObsReceival = simulatedProgress.get(System.currentTimeMillis());
                BeliefNode newBeliefNode = (BeliefNode) newActionNode.receiveObservation(simulatedTimeOfNewObsReceival);
                currRoot = newBeliefNode;
            }

            // this if code omits the evaluation when the same accept is sampled again
            // again
            if (currRoot == null) {
                return;
            }

            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }

    private int calcProgressiveMaxWidth(Node currRoot, Double k, Double a) {
        return Math.max(Double.valueOf(k * Math.pow(currRoot.getVisits(), a)).intValue(), 1);
    }

}
