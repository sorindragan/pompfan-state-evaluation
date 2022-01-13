package geniusweb.pompfan.components;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
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
import geniusweb.pompfan.state.HistoryState;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.pompfan.wideners.AbstractWidener;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressFactory;

public class Tree {
    private static final boolean PARTICLE_DEBUG = false;
    private BeliefNode root;
    private Domain domain;
    @JsonManagedReference
    private UtilitySpace utilitySpace;

    private AbstractBelief belief;

    private AbstractWidener widener; // Used for the action we choose to simulate (expand new node).
    private static final Double C = 2.0; // Math.sqrt(2);
    private ActionNode lastBestActionNode;
    private List<Action> realHistory;
    private Progress progress;
    private Double ACCEPT_SLACK = 0.00;
    private AllBidsList allBidsList;
    BeliefNode initRoot;
    private String lastBestActionNodeInitPartyId = "NoAgent";

    public Tree(UtilitySpace utilitySpace, AbstractBelief belief, AbstractState<?> startState, AbstractWidener widener,
            Progress progress) {
        this.setUtilitySpace(utilitySpace);
        this.belief = belief;
        this.allBidsList = new AllBidsList(this.getUtilitySpace().getDomain());
        
        BeliefNode initalRoot = new BeliefNode(null, startState, null);
        this.initRoot = initalRoot;
        this.setRoot(initalRoot);
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

    public void construct(Long simulationTime, Progress realProgress) throws StateRepresentationException {
        long currentTimeMillis = System.currentTimeMillis(); 
        Progress simulatedProgress = ProgressFactory.create(new DeadlineTime(simulationTime), currentTimeMillis);
       
        // set the new root time
        this.root.getState().setTime(realProgress.get(currentTimeMillis + simulationTime));

        while (Boolean.FALSE.equals(simulatedProgress.isPastDeadline(System.currentTimeMillis()))) {
            // two nodes should be added after each simulation: AN -> BN
            this.simulate(realProgress, simulationTime);
        }
    }

    public void simulate(Progress negProgress, Long shiftSimTime) throws StateRepresentationException {
        Node currRoot = this.root;
        // sample a different opponent and add it to the state
        AbstractPolicy currOpp = this.belief.sampleOpponent();
        this.root.getState().setOpponent(currOpp);
        // main function
        this.widener.widen(negProgress, shiftSimTime, currRoot);
    }

    public static void backpropagate(Node node, Double value) {
        while (node.getParent() != null) {
            node.updateVisits();
            // calculate UCB1 while propagating
            // node.setValue(node.getValue() + value).setValue(UCB1(node));
            
            // update value 
            node.setValue(node.getValue() + value);

            node = node.getParent();
        }
        node.updateVisits();
        // value of the root is not important tho
        node.setValue(node.getValue() + value);
    }

    public static Node selectFavoriteChild(List<Node> candidatesChildrenForAdoption) {

        return candidatesChildrenForAdoption.stream().filter(child -> child.getIsTerminal() == false)
                .max(Comparator.comparing(Tree::UCB1)).orElse(null);

    }

    public Tree receiveRealObservation(Action observationAction, Long time) {

        Offer newRealObservation = (Offer) observationAction;

        // this should always happen when data collection is on
        Offer lastRealAgentAction = this.realHistory.size() >= 2
                ? (Offer) this.realHistory.get(this.realHistory.size() - 2)
                : null;
        Offer lastRealOpponentAction = this.realHistory.size() >= 3
                ? (Offer) this.realHistory.get(this.realHistory.size() - 3)
                : null;
        Offer second2lastAgentAction = this.realHistory.size() >= 4
                ? (Offer) this.realHistory.get(this.realHistory.size() - 4)
                : null;

        // get the real negotiation time
        // this might be wrong as the state has the simulated history, not the real one!
        AbstractState<?> stateUpdatedWithRealTime = this.root.getState().copyState().setTime(this.getProgress().get(time));
        
        // sanity passed
        // System.out.println("SANITY");
        // System.out.println(this.getProgress().get(time));
        // System.out.println(lastRealAgentAction);
        // System.out.println(lastRealOpponentAction);
        // HistoryState tmp = (HistoryState) stateUpdatedWithRealTime;
        // System.out.println(tmp.getHistory().size());
       
        // OBS: the history in the state is now depricated!
        // it will be updated on line :216 => don't use the history inside the state
        // update the belief based on real observation
        this.belief = this.belief.updateBeliefs(
                newRealObservation, 
                lastRealAgentAction, 
                lastRealOpponentAction,
                second2lastAgentAction,
                stateUpdatedWithRealTime);

        if (PARTICLE_DEBUG) {
            System.out.println("New Belief-Probabilities");
            System.out.println(this.belief);
        }

        // if the lastBestActionNode is the inital one
        if (this.lastBestActionNode.getAction().getActor().getName()
                .compareTo(this.lastBestActionNodeInitPartyId) == 0) {
            // Startphase - opponent bids first
            // Quickfix: by doing nothing
            return this;
        }

        List<Node> rootCandidates = this.lastBestActionNode.getChildren().stream()
                .filter(node -> node.getIsTerminal() == false).collect(Collectors.toList());

        if (rootCandidates.isEmpty()) {
            // Downgrade the value of accept nodes in order to facilitate exploration by
            // forcing a root change
            this.lastBestActionNode.setValue(this.lastBestActionNode.getValue() - 1.0);
            // try again
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
        
        // in case of the real observation needs to be forced inside the history state
        // not used as a top-down change to all the states in all the deeper levels ndoes would be necessary
        // Action simulatedAction = nextRoot.getStoredAction();
        // ArrayList<Action> updatedHistory = ((HistoryState) nextRoot.getState()).getHistory();
        // updatedHistory.set(updatedHistory.indexOf(simulatedAction), newRealObservation);

        // HistoryState updatedState = (HistoryState) nextRoot.getState().copyState();
        // updatedState.setHistory(updatedHistory);
        // nextRoot.setState(updatedState);

        // Exchange the simulated observation with the real one for future simulations
        BeliefNode updatedRoot = (BeliefNode) nextRoot;
        updatedRoot.setObservation(newRealObservation);
        // changing the root
        this.setRoot(updatedRoot);
        // discard rest of the tree
        this.getRoot().setParent(null);

        return this;
    }

    public Action chooseBestAction() {

        List<Node> oldestChildren = this.root.getChildren();
        Action action = null;

        do {

            if (oldestChildren.isEmpty()) {
                // should not happen
                return null;
            }

            this.setLastBestActionNode((ActionNode) oldestChildren.stream()
                    .max(Comparator.comparing(node -> node.getValue())).get());
            action = this.getLastBestActionNode().getAction();

            if (this.realHistory.isEmpty()) {
                return action;
            }
            Action lastOpponentAction = this.realHistory.get(this.realHistory.size() - 1);

            Bid lastOpponentBid = ((Offer) lastOpponentAction).getBid();
            // if this is done, there is no need to insert accepts in the tree
            // it just complicates things
            // having accepts in the tree should provide, under a good enough eval function, enough information
            // for letting the MCTS take the decision between Accepting or Offering
            Bid futureAgentBid = action instanceof Accept ? ((Accept) action).getBid() : ((Offer) action).getBid();
            Double distanceValue = this.getUtilitySpace().getUtility(futureAgentBid)
                    .subtract(this.getUtilitySpace().getUtility(lastOpponentBid)).doubleValue();

            // if we want to propose something worse than what we received
            if (distanceValue + ACCEPT_SLACK < 0) {
                action = new Accept(action.getActor(), lastOpponentBid);
                System.out.println("MCTS SENT ACCEPT :)");
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
        Deque<Node> nodeStack = new ArrayDeque<>();
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
        // this.buildStringBuffer(buffer, "", "", this.initRoot);

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
        this.root.setParent(null)
        .setChildren(new ArrayList<Node>())
        .setValue(0.0).setVisits(0);
    }

}
