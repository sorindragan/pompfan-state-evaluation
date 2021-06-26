package geniusweb.custom.components;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.beliefs.AbstractBelief;
import geniusweb.custom.evaluators.EvaluationFunctionInterface;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.HistoryState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

// Tree<T extends AbstractState<?>>
public class Tree {
    private BeliefNode root;
    private static Random random = new Random(42);
    private Domain domain;
    private AbstractBelief belief;
    private Integer maxWidth;
    private AbstractOwnExplorationPolicy ownExplorationStrategy; // The action we choose to simulate (expand new node).
    private EvaluationFunctionInterface evaluator;
    private static Double C = Math.sqrt(2); // TODO: Make it hyperparam
    private ActionNode lastBestActionNode;

    public Tree(UtilitySpace utilitySpace, AbstractBelief belief, Integer maxWidth, AbstractState<?> startState,
            AbstractOwnExplorationPolicy ownPolicy) {
        // this.evaluator = evaluationFunction;
        this.belief = belief;
        this.maxWidth = maxWidth;
        this.root = new BeliefNode(null, startState, null);
        this.ownExplorationStrategy = ownPolicy;

    }

    public void simulate() throws StateRepresentationException {
        Node currRoot = this.root;
        AbstractPolicy currOpp = this.belief.sampleOpponent();
        currRoot.getState().setOpponent(currOpp);
        while (currRoot.getChildren().size() == this.maxWidth) { // yes the fuck
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            if (currRoot.getChildren().size() < this.maxWidth) {
                currRoot = (BeliefNode) ((ActionNode) currRoot).receiveObservation();
                Action opponentAction = ((BeliefNode) currRoot).getObservation();
                Action agentAction = ((ActionNode) currRoot.getParent()).getAction();
                Double value = currRoot.getState().evaluate();
                return;
            } else {
                // Going down the tree
                currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            }
        }

        if (currRoot.getChildren().size() < this.maxWidth) {
            ActionNode actionNode = (ActionNode) ((BeliefNode) currRoot).act(ownExplorationStrategy); // What the fuck
            BeliefNode beliefNode = (BeliefNode) actionNode.receiveObservation();
            currRoot = beliefNode;
            Double value = currRoot.getState().evaluate();
            Tree.backpropagate(currRoot, value);
        }
    }

    private static void backpropagate(Node node, Double value) {
        while (node.getParent() != null) {
            node.setVisits(node.getVisits() + 1);
            node.setValue(node.getValue() + value);
            node = node.getParent();
        }
        node.setVisits(node.getVisits() + 1);
        node.setValue(node.getValue() + value);
    }

    public Double evaluate(AbstractState<?> state, Action opponentAction, Action agentAction) { // TODO: Not needed
                                                                                                // anymore?
        Bid lastBid = ((Offer) opponentAction).getBid();
        Bid secondTolastBid = ((Offer) agentAction).getBid();
        return this.evaluator.evaluate(state, lastBid, secondTolastBid);
    }

    // public static Node selectFavoriteChild(List<Node>
    // candidatesChildrenForAdoption) {
    // // True Random - Alt.: Proportional to the visits
    // Node adoptedChild = candidatesChildrenForAdoption
    // .get(Tree.random.nextInt(candidatesChildrenForAdoption.size()));
    // return adoptedChild;
    // }

    public static Node selectFavoriteChild(List<Node> candidatesChildrenForAdoption) {
        // True Random - Alt.: Proportional to the visits
        Node adoptedChild = candidatesChildrenForAdoption.stream().max(Comparator.comparing(Tree::UCB1)).get();
        return adoptedChild;
    }

    public Tree receiveRealObservation(Action observationAction) {
        List<Node> rootCandidates = this.lastBestActionNode.getChildren();
        this.belief = this.belief.updateBeliefs((Offer) observationAction, (Offer) this.lastBestActionNode.getAction(),
                this.lastBestActionNode.getState());
        List<Bid> candidateBids = rootCandidates.stream().map(node -> ((BeliefNode) node))
                .map(beliefNode -> ((Offer) beliefNode.getObservation())).map(offer -> offer.getBid())
                .collect(Collectors.toList());
        Bid realBid = ((Offer) observationAction).getBid();
        Bid closestBid = this.belief.getDistance().computeMostSimilar(realBid, candidateBids);
        Node nextRoot = rootCandidates.parallelStream()
                .filter(node -> ((Offer) ((BeliefNode) node).getObservation()).getBid() == closestBid).findFirst().get();
        this.root = (BeliefNode) nextRoot;
        return this;
    }

    public void construct(Integer maxIter) throws StateRepresentationException {
        Integer currIter = 0;
        while (currIter < maxIter) {
            this.simulate();
            currIter++;
        }
    }

    public Action chooseBestAction() {
        List<Node> oldestChildren = this.root.getChildren();
        this.lastBestActionNode = (ActionNode) oldestChildren.stream()
                .max(Comparator.comparing(node -> node.getValue())).get();
        Action action = lastBestActionNode.getAction();
        return action;
    }

    public static Double UCB1(Node node) {
        Double val = node.getValue();
        Double visits = node.getVisits().doubleValue();
        Double pVisits = node.getParent().getVisits().doubleValue();
        return (val / visits) + (C * Math.sqrt(Math.log(pVisits + 1) / visits));
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        this.buildStringBuffer(buffer, "", "", this.root);
        return buffer.toString();
    }

    private void buildStringBuffer(StringBuilder buffer, String prefix, String childrenPrefix, Node root) {
        buffer.append(prefix);
        buffer.append(root.toString());
        buffer.append('\n');
        for (Iterator<Node> it = root.getChildren().iterator(); it.hasNext();) {
            Node next = it.next();
            if (it.hasNext()) {
                buildStringBuffer(buffer, childrenPrefix + "├── ", childrenPrefix + "│   ", next);
            } else {
                buildStringBuffer(buffer, childrenPrefix + "└── ", childrenPrefix + "    ", next);
            }
        }
    }

}
