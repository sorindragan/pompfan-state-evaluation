package geniusweb.custom.components;

import java.math.BigDecimal;
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
import geniusweb.custom.wideners.AbstractWidener;
import geniusweb.deadline.DeadlineTime;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressFactory;

// Tree<T extends AbstractState<?>>
public class Tree {
    private static final boolean DEBUG = true;
    private BeliefNode root;
    private static Random random = new Random(42);
    private Domain domain;
    private UtilitySpace utilitySpace;

    private AbstractBelief belief;
    private Integer maxWidth;

    private AbstractWidener widener; // The action we choose to simulate (expand new node).
    private EvaluationFunctionInterface evaluator;
    private static Double C = Math.sqrt(2); // TODO: Make it hyperparam
    private ActionNode lastBestActionNode;
    private Progress progress;
    private Double currentTime = 0.0;

    public Tree(UtilitySpace utilitySpace, AbstractBelief belief, Integer maxWidth, AbstractState<?> startState,
            AbstractWidener widener, Progress progress) {
        // this.evaluator = evaluationFunction;
        this.belief = belief;
        this.maxWidth = maxWidth;
        this.root = new BeliefNode(null, startState, null);
        this.widener = widener;
        this.setProgress(progress); // Around two seconds
        // TODO: Does it make sense to sample a new opponent everytime we simulate. This part was at the beginning of simulate
        AbstractPolicy currOpp = this.belief.sampleOpponent(); 
        this.root.getState().setOpponent(currOpp);

    }

    public void simulate(Progress simulatedProgress) throws StateRepresentationException {
        Node currRoot = this.root;
        this.widener.widen(simulatedProgress, currRoot);
    }

    public static void backpropagate(Node node, Double value) {
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
        Node adoptedChild = candidatesChildrenForAdoption.stream().filter(child -> child.getIsTerminal() == false)
                .max(Comparator.comparing(Tree::UCB1)).get();
        return adoptedChild;
    }

    public Tree receiveRealObservation(Action observationAction, Long time) {
        this.currentTime = this.getProgress().get(time);
        this.belief = this.belief.updateBeliefs((Offer) observationAction, (Offer) this.lastBestActionNode.getAction(),
                this.lastBestActionNode.getState().setRound(this.currentTime));

        if (DEBUG) {
            System.out.println("New Belief-Probabilities");
            System.out.println(this.belief);
        }

        List<Node> rootCandidates = this.lastBestActionNode.getChildren().stream()
                .filter(node -> node.getIsTerminal() == false).collect(Collectors.toList());
        if (rootCandidates.size() == 0) {
            // Quickfix by doing nothing!
            return this;
        }

        List<Bid> candidateBids = rootCandidates.stream().map(node -> ((BeliefNode) node))
                .map(beliefNode -> ((Offer) beliefNode.getObservation())).map(offer -> offer.getBid())
                .collect(Collectors.toList());
        // System.out.println(candidateBids);
        Bid realBid = ((Offer) observationAction).getBid();
        Bid closestBid = this.belief.getDistance().computeMostSimilar(realBid, candidateBids);
        Node nextRoot = rootCandidates.parallelStream()
                .filter(node -> ((Offer) ((BeliefNode) node).getObservation()).getBid() == closestBid).findFirst()
                .get();
        this.root = (BeliefNode) nextRoot;
        // try {
        // } catch (Exception e) {
        // e.printStackTrace();
        // System.exit(0);

        // }
        return this;
    }

    public void construct(Long simulationTime) throws StateRepresentationException {
        Progress simulatedProgress = ProgressFactory.create(new DeadlineTime(simulationTime),
                System.currentTimeMillis());
        int currIter = 0;
        // System.out.println(simulatedProgress.getTerminationTime());
        while (simulatedProgress.isPastDeadline(System.currentTimeMillis()) == false) {
            this.simulate(simulatedProgress);
            currIter++;
            // System.out.println(currIter);
        }
    }

    public Action chooseBestAction() {
        List<Node> oldestChildren = this.root.getChildren();
        this.lastBestActionNode = (ActionNode) oldestChildren.stream()
                .max(Comparator.comparing(node -> node.getValue())).get();
        Action action = lastBestActionNode.getAction();
        // System.out.println("Choose...");
        // System.out.println(lastBestActionNode);
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

    public Domain getDomain() {
        return domain;
    }

    public Tree setDomain(Domain domain) {
        this.domain = domain;
        return this;
    }

    public AbstractBelief getBelief() {
        return belief;
    }

    public Tree setBelief(AbstractBelief belief) {
        this.belief = belief;
        return this;
    }

    public Integer getMaxWidth() {
        return maxWidth;
    }

    public Tree setMaxWidth(Integer maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public EvaluationFunctionInterface getEvaluator() {
        return evaluator;
    }

    public Tree setEvaluator(EvaluationFunctionInterface evaluator) {
        this.evaluator = evaluator;
        return this;
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public Tree setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
        return this;
    }

    public Progress getProgress() {
        return progress;
    }

    public Tree setProgress(Progress progress) {
        this.progress = progress;
        return this;
    }

}
