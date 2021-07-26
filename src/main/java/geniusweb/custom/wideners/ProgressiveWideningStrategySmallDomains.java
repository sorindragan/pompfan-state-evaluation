package geniusweb.custom.wideners;

import java.util.Comparator;
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
                Double value;
                // If the bid space is not large enough, it happens that we expand paths that are already present in the nodes,
                // so we go with the paths, but add 2 new nodes at the end of it; this happens only if an offer is sampled from the opponent
                int explorationTries = 0;
                while (currActionNode.getChildren().contains(receivedObservationNode) && explorationTries < 100) {
                    explorationTries++;
                    Action possibleFutureObservation = receivedObservationNode.getObservation();
                    if (possibleFutureObservation instanceof Accept) {
                        
                        // Still evaluate as an accept and update the rest of the tree
                        // If the next 3 lines are commented, the number of children and the number of visits will resonate
                        // Bid acceptBid = ((Accept) possibleFutureObservation).getBid();
                        // value = this.getOwnExplorationStrategy().getUtilitySpace().getUtility(acceptBid).doubleValue();
                        // Tree.backpropagate(currRoot, value);
                        System.out.println("CONTAINED: EXIT ACC count:" + explorationTries);

                        return;
                    }

                    // extra check not needed; the possibleFutureObservation should be an Offer if not an Accept
                    // if (possibleFutureObservation instanceof Offer) {}
                    simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                    currActionNode = (ActionNode) receivedObservationNode.act(this.getOwnExplorationStrategy(), simulatedTimeOfObsReceival);
                    receivedObservationNode = (BeliefNode) currActionNode.receiveObservation(simulatedTimeOfObsReceival);
                    currRoot = receivedObservationNode; 
                }
                
                if (explorationTries > 0) {
                    System.out.println("CONTAINED: EXIT count:" + explorationTries);
                }
                
                value = currRoot.getState().evaluate();
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
            
            int explorationTries = 0;
            while(nextActionNode.getChildren().contains(beliefNode) && explorationTries < 100) {
                // System.out.println("CONTAINED2");
                explorationTries++;
                Action possibleFutureObservation = beliefNode.getObservation();
                if (possibleFutureObservation instanceof Accept) {

                    // Still evaluate as an accept and update the rest of the tree
                    // If the next 3 lines are commented, the number of children and the number of
                    // visits will resonate
                    // Bid acceptBid = ((Accept) possibleFutureObservation).getBid();
                    // value =
                    // this.getOwnExplorationStrategy().getUtilitySpace().getUtility(acceptBid).doubleValue();
                    // Tree.backpropagate(currRoot, value);
                    System.out.println("CONTAINED2: EXIT ACC count:" + explorationTries);

                    return;
                }

                // extra check not needed; the possibleFutureObservation should be an Offer if
                // not an Accept
                // if (possibleFutureObservation instanceof Offer) {}
                simulatedTimeOfObsReceival = simulatedProgress.get(System.currentTimeMillis());
                nextActionNode = (ActionNode) beliefNode.act(this.getOwnExplorationStrategy(),
                        simulatedTimeOfObsReceival);
                beliefNode = (BeliefNode) nextActionNode
                        .receiveObservation(simulatedTimeOfObsReceival);
                currRoot = beliefNode;
            }
            if (explorationTries > 0) {
                System.out.println("CONTAINED2: EXIT count:" + explorationTries);
            }
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }

    private int calcProgressiveMaxWidth(Node currRoot, Double k, Double a) {
        return Double.valueOf(k * Math.pow(currRoot.getVisits(), a)).intValue() + 1;
    }

}
