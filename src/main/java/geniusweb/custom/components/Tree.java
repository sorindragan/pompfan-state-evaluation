package geniusweb.custom.components;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import geniusweb.actions.Action;
import geniusweb.custom.beliefs.AbstractBelief;
import geniusweb.custom.evaluators.AbstractEvaluationFunction;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.HistoryState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

// Tree<T extends AbstractState<?>>
public class Tree {
    private BeliefNode root;
    private static Random random = new Random(42);
    private Domain domain;
    private AbstractBelief belief;
    private Integer maxWidth;
    private AbstractPolicy ownExplorationStrategy; // The action we choose to simulate (expand new node).
    private AbstractEvaluationFunction evaluator;
    private static Double C = Math.sqrt(2); // TODO: Make it hyperparam

    public Tree(Domain domain, AbstractBelief belief, Integer maxWidth, AbstractEvaluationFunction evaluationFunction) {
        this.evaluator = evaluationFunction;
        this.belief = belief;
        this.maxWidth = maxWidth;
        this.root = new BeliefNode(null, new HistoryState(domain, null), null);
    }

    public void simulate() throws StateRepresentationException {
        Node currRoot = this.root;
        AbstractPolicy currOpp = this.belief.sampleOpponent();
        currRoot.getState().setOpponent(currOpp);
        while (currRoot.getChildren().size() == this.maxWidth) { // yes the fuck
            currRoot = Tree.selectFavoriteChild(currRoot.getChildren());
            if (currRoot.getChildren().size() < this.maxWidth) {
                currRoot = (BeliefNode) ((ActionNode) currRoot).receiveObservation();
                Double value = this.evaluate(currRoot.getState());
                Tree.backpropagate(currRoot, value);
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
            Double value = this.evaluate(currRoot.getState());
            Tree.backpropagate(currRoot, value);
        }
    }

    private static void backpropagate(Node node, Double value) {
        while (node.getParent() != null) {
            node.setVisits(node.getVisits() + 1);
            node.setValue(node.getValue() + value);
            node = node.getParent();
        }
    }

    public Double evaluate(AbstractState<?> state) {
        return this.evaluator.evaluate(state);
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

    public void construct(Integer maxIter) throws StateRepresentationException {
        Integer currIter = 0;
        while (currIter < maxIter) {
            this.simulate();
            currIter++;
        }
    }

    public Action chooseBestAction() {
        List<Node> oldestChildren = this.root.getChildren();
        ActionNode adoptedChild = (ActionNode) oldestChildren.stream().max(Comparator.comparing(node -> node.getValue())).get();
        Action action = adoptedChild.getAction();
        return action;
    }

    public static Double UCB1(Node node) {
        Double val = node.getValue();
        Double visits = node.getVisits().doubleValue();
        Double pVisits = node.getParent().getVisits().doubleValue();
        return (val / visits) + (C * Math.sqrt(Math.log(pVisits + 1) / visits));
    }

}
