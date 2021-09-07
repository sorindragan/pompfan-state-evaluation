package geniusweb.pompfan.components;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.deadline.DeadlineTime;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.beliefs.AbstractBelief;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.pompfan.wideners.AbstractWidener;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressFactory;

public class Tree {
    private static final boolean PARTICLE_DEBUG = false;
    private static final boolean TREE_DEBUG = false;
    private BeliefNode root;
    private Domain domain;
    @JsonManagedReference
    private UtilitySpace utilitySpace;

    private AbstractBelief belief;

    private AbstractWidener widener; // Used for the action we choose to simulate (expand new node).
    private static Double C = 2.0; // Math.sqrt(2);
    private ActionNode lastBestActionNode;
    private List<Action> realHistory;
    private Progress progress;
    private Double currentTime = 0.0;
    private BeliefNode originalRoot;
    private Double ACCEPT_SLACK = 0.05;
    private AllBidsList allBidsList;
    private String lastBestActionNodeInitPartyId = "NoAgent";

    public Tree(UtilitySpace utilitySpace, AbstractBelief belief, AbstractState<?> startState, AbstractWidener widener,
            Progress progress) {
        this.setUtilitySpace(utilitySpace);
        this.belief = belief;
        BeliefNode tmpRoot = new BeliefNode(null, startState, null);

        if (TREE_DEBUG)
            this.originalRoot = tmpRoot;

        this.setRoot(tmpRoot);
        this.allBidsList = new AllBidsList(this.getUtilitySpace().getDomain());
        // This is a very bad bit of code: hardcoded initialization of lastBestAction
        // node
        this.lastBestActionNode = new ActionNode(null, startState,
                new Offer(new PartyId(lastBestActionNodeInitPartyId), this.allBidsList.get(BigInteger.ZERO)));

        this.widener = widener;
        this.realHistory = new ArrayList<Action>();
        this.setProgress(progress);

    }

    public BeliefNode getRoot() {
        return root;
    }

    public void setRoot(BeliefNode root) {
        this.root = root;
    }

    public ActionNode getLastBestActionNode() {
        return lastBestActionNode;
    }

    public void setLastBestActionNode(ActionNode lastBestActionNode) {
        this.lastBestActionNode = lastBestActionNode;
    }

    public List<Action> getRealHistory() {
        return realHistory;
    }

    public void setRealHistory(List<Action> realHistory) {
        this.realHistory = realHistory;
    }

    public void setOriginalRoot(BeliefNode originalRoot) {
        this.originalRoot = originalRoot;
    }

    public BeliefNode getOriginalRoot() {
        return originalRoot;
    }

    public void construct(Long simulationTime, Progress realProgress) throws StateRepresentationException {
        long currentTimeMillis = System.currentTimeMillis(); 
        Progress simulatedProgress = ProgressFactory.create(new DeadlineTime(simulationTime), currentTimeMillis);
        Progress realShiftedProgress = ProgressFactory.create(
                new DeadlineTime(realProgress.getTerminationTime().getTime() - currentTimeMillis - simulationTime),
                currentTimeMillis + simulationTime);
        while (simulatedProgress.isPastDeadline(System.currentTimeMillis()) == false) {
            // two nodes should be added after each simulation: AN -> BN
            this.simulate(realShiftedProgress, simulationTime);
        }
    }

    public void simulate(Progress simulatedProgress, Long shiftSimTime) throws StateRepresentationException {
        Node currRoot = this.root;
        // sample a different opponent and add it to the state
        AbstractPolicy currOpp = this.belief.sampleOpponent();
        this.root.getState().setOpponent(currOpp);
        this.widener.widen(simulatedProgress, shiftSimTime, currRoot);
    }

    public static void backpropagate(Node node, Double value) {
        while (node.getParent() != null) {
            node.updateVisits();
            node.setValue(node.getValue() + value);
            // calculate UCB1
            node.setValue(UCB1(node));
            node = node.getParent();
        }
        node.updateVisits();
        // value of the root is not important
        node.setValue(node.getValue() + value);
    }

    public static Node selectFavoriteChild(List<Node> candidatesChildrenForAdoption) {

        if (candidatesChildrenForAdoption.stream().allMatch(child -> child.getIsTerminal() == true)) {
            return null;
        }

        return candidatesChildrenForAdoption.stream().filter(child -> child.getIsTerminal() == false)
                .max(Comparator.comparing(Node::getValue)).get();

    }

    public Tree receiveRealObservation(Action observationAction, Long time) {

        this.currentTime = this.getProgress().get(time);
        Offer newRealObservation = (Offer) observationAction;

        // this should always happen when data collection is on
        Offer lastRealOpponentAction = this.realHistory.size() >= 3
                ? (Offer) this.realHistory.get(this.realHistory.size() - 3)
                : null;
        Offer lastRealAgentAction = this.realHistory.size() >= 3
                ? (Offer) this.realHistory.get(this.realHistory.size() - 2)
                : null;

        /*
         * Offer lastRealAgentAction = this.lastBestActionNode != null ? (Offer)
         * this.lastBestActionNode.getAction() : null; if (lastRealAgentAction == null)
         * { // we should construct the tree first return this; }
         */
        // get the real negotiation time
        AbstractState<?> stateUpdatedWithRealTime = this.lastBestActionNode.getState().copyState().setTime(this.currentTime);
        // update the belief based on real observation
        this.belief = this.belief.updateBeliefs(newRealObservation, lastRealAgentAction, lastRealOpponentAction,
                stateUpdatedWithRealTime);

        if (PARTICLE_DEBUG) {
            System.out.println(this.belief);
            System.out.println("New Belief-Probabilities");
        }

        // if the lastBestActionNode is the inital one
        if (this.lastBestActionNode.getAction().getActor().getName()
                .compareTo(this.lastBestActionNodeInitPartyId) == 0) {
            // Quickfix: by doing nothing
            // Startphase - opponent bids first
            return this;
        }

        List<Node> rootCandidates = this.lastBestActionNode.getChildren().stream()
                .filter(node -> node.getIsTerminal() == false).collect(Collectors.toList());

        if (rootCandidates.size() == 0) {
            // Downgrade the value of accept nodes in order to facilitate exploration by
            // forcing a root change
            this.lastBestActionNode.setValue(this.lastBestActionNode.getValue() - 1.0);
            // Quickfix: by doing nothing!
            return this;
        }

        List<Bid> candidateBids = rootCandidates.stream().map(node -> ((BeliefNode) node))
                .map(beliefNode -> ((Offer) beliefNode.getObservation())).map(offer -> offer.getBid())
                .collect(Collectors.toList());
        Bid realBid = ((Offer) observationAction).getBid();
        Bid closestBid = this.belief.getDistance().computeMostSimilar(realBid, candidateBids);
        Node nextRoot = rootCandidates.parallelStream()
                .filter(node -> ((Offer) ((BeliefNode) node).getObservation()).getBid() == closestBid).findFirst()
                .get();

        // changing the root
        this.setRoot((BeliefNode) nextRoot);
        this.getRoot().setParent(null);

        return this;
    }

    public Action chooseBestAction() {

        List<Node> oldestChildren = this.root.getChildren();
        Action action = null;

        do {

            if (oldestChildren.isEmpty()) {
                return null;
            }

            this.lastBestActionNode = (ActionNode) oldestChildren.stream()
                    .max(Comparator.comparing(node -> node.getValue())).get();
            action = lastBestActionNode.getAction();

            if (this.realHistory.size() == 0) {
                return action;
            }
            Action lastOpponentAction = this.realHistory.get(this.realHistory.size() - 1);

            Bid lastOpponentBid = ((Offer) lastOpponentAction).getBid();
            Bid futureAgentBid = action instanceof Accept ? ((Accept) action).getBid() : ((Offer) action).getBid();
            Double distanceValue = this.getUtilitySpace().getUtility(futureAgentBid)
                    .subtract(this.getUtilitySpace().getUtility(lastOpponentBid)).doubleValue();

            // if we want to propose something worse than what we received
            if (distanceValue + ACCEPT_SLACK < 0) {
                action = new Accept(action.getActor(), lastOpponentBid);
                break;
            }
            if (action instanceof Accept) {
                // not a good enough accept node for this point
                this.root.getChildren().remove(this.lastBestActionNode);
            }
        } while (action instanceof Accept);

        return action;
    }

    public static Double UCB1(Node node) {
        Double val = node.getValue();
        Double visits = node.getVisits().doubleValue();
        Double pVisits = node.getParent().getVisits().doubleValue();
        return (val / visits) + (C * Math.sqrt(Math.log(pVisits + 1) / visits));
    }

    public int howManyNodes() {
        int nodeNumber = 0;
        Node node;
        Stack<Node> nodeStack = new Stack<>();
        nodeStack.push(this.root);

        while (!nodeStack.isEmpty()) {
            node = nodeStack.pop();
            nodeNumber++;
            for (Node child : node.getChildren()) {
                nodeStack.push(child);
            }
        }
        return nodeNumber;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(50);
        this.buildStringBuffer(buffer, "", "", this.root);
        return buffer.toString();
    }

    public String toStringOriginal() {
        StringBuilder buffer = new StringBuilder(50);
        this.buildStringBuffer(buffer, "", "", this.getOriginalRoot());
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

    public void scrapeSubTree() {
        this.root.setChildren(new ArrayList<Node>());
        this.root.setValue(0.0).setVisits(0);
        // this.root.getState().setTime(0.0);
        // this.root.getState().setTime(this.progress.get(System.currentTimeMillis()));
    }

}
