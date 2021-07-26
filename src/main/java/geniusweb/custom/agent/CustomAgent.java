package geniusweb.custom.agent; // TODO: change name

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import geniusweb.actions.FileLocation;

import java.util.UUID;
import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.LearningDone;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.custom.beliefs.AbstractBelief;
import geniusweb.custom.beliefs.ParticleFilterBelief;
import geniusweb.custom.beliefs.ParticleFilterWithAcceptBelief;
import geniusweb.custom.beliefs.UniformBelief;
import geniusweb.custom.components.Tree;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.distances.UtilityBidDistance;
import geniusweb.custom.evaluators.EvaluationFunctionInterface;
import geniusweb.custom.evaluators.Last2BidsMeanUtilityEvaluator;
import geniusweb.custom.evaluators.Last2BidsProductUtilityEvaluator;
import geniusweb.custom.evaluators.RandomEvaluator;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.explorers.RandomNeverAcceptOwnExplorationPolicy;
import geniusweb.custom.explorers.RandomOwnExplorerPolicy;
import geniusweb.custom.explorers.SelfishNeverAcceptOwnExplorerPolicy;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.opponents.AntagonisticOpponentPolicy;
import geniusweb.custom.opponents.BoulwareOpponentPolicy;
import geniusweb.custom.opponents.ConcederOpponentPolicy;
import geniusweb.custom.opponents.HardLinerOpponentPolicy;
import geniusweb.custom.opponents.RandomOpponentPolicy;
import geniusweb.custom.opponents.SelfishOpponentPolicy;
import geniusweb.custom.opponents.TimeDependentOpponentPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.HistoryState;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.custom.wideners.AbstractWidener;
import geniusweb.custom.wideners.MaxWidthWideningStrategy;
import geniusweb.custom.wideners.ProgressiveWideningStrategy;
import geniusweb.exampleparties.boulware.Boulware;
import geniusweb.exampleparties.linear.Linear;
import geniusweb.exampleparties.timedependentparty.TimeDependentParty;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Agreements;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import tudelft.utilities.logging.Reporter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomAgent extends DefaultParty { // TODO: change name

    /**
     *
     */
    private static final int NUM_SIMULATIONS = 100;
    private static final int MAX_WIDTH = 10;
    private Long SIMULATION_TIME = 250l; // TODO: BUG if increased
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PRINT_TREE = false;
    private static final boolean DEBUG_IN_TOURNAMENT = true;
    private Bid lastReceivedBid = null;
    private PartyId me;
    private final Random random = new Random();
    protected ProfileInterface profileint = null;
    private Progress progress;
    private String protocol;
    private Parameters parameters;
    private UtilitySpace uSpace;
    private PersistentState persistentState;
    private NegotiationData negotiationData;
    private List<File> dataPaths;
    private File persistentPath;
    private String opponentName;
    private Tree MCTS;

    public CustomAgent() { // TODO: change name
    }

    public CustomAgent(Reporter reporter) { // TODO: change name
        super(reporter); // for debugging
    }

    /**
     * This method mostly contains utility functionallity for the agent to function
     * properly. The code that is of most interest for the ANL competition is
     * further below and in the other java files in this directory. It does,
     * however, not hurt to read through this code to have a better understanding of
     * what is going on.
     * 
     * @param info information object for agent
     */
    @Override
    public void notifyChange(Inform info) {
        if (DEBUG_IN_TOURNAMENT == false) {
            System.out.println("RECEIVE INFO");
            System.out.println(info.getClass().getName());
        }
        try {

            if (info instanceof Settings) {
                // info is a Settings object that is passed at the start of a negotiation
                Settings settings = (Settings) info;

                // ID of my agent
                this.me = settings.getID();

                // The progress object keeps track of the deadline
                this.progress = settings.getProgress();

                // Protocol that is initiate for the agent
                this.protocol = settings.getProtocol().getURI().getPath();

                // Parameters for the agent (can be passed through the GeniusWeb GUI, or a
                // JSON-file)
                this.parameters = settings.getParameters();

                // The PersistentState is loaded here (see 'PersistenData,java')
                if (this.parameters.containsKey("persistentstate"))
                    this.persistentPath = new FileLocation(
                            UUID.fromString((String) this.parameters.get("persistentstate"))).getFile();
                if (this.persistentPath != null && this.persistentPath.exists()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    this.persistentState = objectMapper.readValue(this.persistentPath, PersistentState.class);
                } else {
                    this.persistentState = new PersistentState();
                }

                // The negotiation data paths are converted here from List<String> to List<File>
                // for improved usage. For safety reasons, this is more comprehensive than
                // normally.
                if (this.parameters.containsKey("negotiationdata")) {
                    List<String> dataPaths_raw = (List<String>) this.parameters.get("negotiationdata");
                    this.dataPaths = new ArrayList<>();
                    for (String path : dataPaths_raw)
                        this.dataPaths.add(new FileLocation(UUID.fromString(path)).getFile());
                }
                if ("Learn".equals(protocol)) {
                    // We are in the learning step: We execute the learning and notify when we are
                    // done. REMEMBER that there is a deadline of 60 seconds for this step.
                    learn();
                    getConnection().send(new LearningDone(me));
                } else {
                    // We are in the negotiation step.

                    // Create a new NegotiationData object to store information on this negotiation.
                    // See 'NegotiationData.java'.
                    this.negotiationData = new NegotiationData();

                    // Obtain our utility space, i.e. the problem we are negotiating and our
                    // preferences over it.
                    try {
                        this.profileint = ProfileConnectionFactory.create(settings.getProfile().getURI(),
                                getReporter());
                        this.uSpace = ((UtilitySpace) profileint.getProfile());
                        // Our stuff

                        Domain domain = this.uSpace.getDomain();
                        List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();

                        // Object opponents = this.parameters.get("opponents");

                        for (int cnt = 0; cnt < 100; cnt++) {
                            listOfOpponents.add(new AntagonisticOpponentPolicy(this.uSpace));
                            listOfOpponents.add(new SelfishOpponentPolicy(domain));
                            listOfOpponents.add(new TimeDependentOpponentPolicy(domain));
                            listOfOpponents.add(new HardLinerOpponentPolicy(domain));
                            listOfOpponents.add(new ConcederOpponentPolicy(domain));
                            listOfOpponents.add(new BoulwareOpponentPolicy(domain));
                        }

                        AbstractBidDistance distance = new UtilityBidDistance(this.uSpace);
                        // AbstractBelief belief = new ParticleFilterBelief(listOfOpponents, distance);
                        AbstractBelief belief = new ParticleFilterWithAcceptBelief(listOfOpponents, distance);
                        // AbstractBelief belief = new UniformBelief(listOfOpponents, distance);
                        // DONE: Check if belief is updated -- It is!
                        EvaluationFunctionInterface<HistoryState> evaluator = new Last2BidsProductUtilityEvaluator(
                                this.uSpace);
                        AbstractState<?> startState = new HistoryState(this.uSpace, null, evaluator);
                        // AbstractOwnExplorationPolicy explorer = new
                        // SelfishNeverAcceptOwnExplorerPolicy(domain, this.uSpace, me);
                        AbstractOwnExplorationPolicy explorer = new RandomOwnExplorerPolicy(this.uSpace, me);
                        AbstractWidener widener = new ProgressiveWideningStrategy(explorer, 4.0, 0.1, 4.0, 0.1); // TODO:
                                                                                                                 // BUG
                                                                                                                 // if
                                                                                                                 // increased
                        // AbstractWidener widener = new MaxWidthWideningStrategy(explorer, MAX_WIDTH);
                        this.MCTS = new Tree(this.uSpace, belief, startState, widener, this.progress);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            } else if (info instanceof ActionDone) {
                // The info object is an action that is performed by an agent.
                Action action = ((ActionDone) info).getAction();

                // Check if this is not our own action
                if (!this.me.equals(action.getActor())) {
                    // Check if we already know who we are playing against.
                    if (this.opponentName == null) {
                        // The part behind the last _ is always changing, so we must cut it off.
                        String fullOpponentName = action.getActor().getName();
                        int lastIndexOf = fullOpponentName.lastIndexOf("_");
                        // int index = lastIndexOf == -1 ? fullOpponentName.length() : lastIndexOf;
                        int index = lastIndexOf;
                        this.opponentName = fullOpponentName.substring(0, index);

                        // Add name of the opponent to the negotiation data
                        this.negotiationData.setOpponentName(this.opponentName);
                    }
                    // Process the action of the opponent.
                    processAction(action);
                }
            } else if (info instanceof YourTurn) {
                // Advance the round number if a round-based deadline is set.
                if (progress instanceof ProgressRounds) {
                    progress = ((ProgressRounds) progress).advance();
                }

                // The info notifies us that it is our turn
                myTurn();
            } else if (info instanceof Finished) {
                // The info is a notification that th negotiation has ended. This Finished
                // object also contains the final agreement (if any).
                Agreements agreements = ((Finished) info).getAgreement();
                processAgreements(agreements);
                if (DEBUG_PRINT_TREE) {
                    FileWriter fullTreeFileWriter = new FileWriter("logs/log_fullTree.txt");
                    fullTreeFileWriter.write(this.MCTS.toStringOriginal());
                    fullTreeFileWriter.close();
                    FileWriter currRootFileWriter = new FileWriter("logs/log_currRoot.txt");
                    currRootFileWriter.write(this.MCTS.toString());
                    currRootFileWriter.close();
                }
                // Write the negotiation data that we collected to the path provided.
                if (this.dataPaths != null && this.negotiationData != null) {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.writerWithDefaultPrettyPrinter().writeValue(this.dataPaths.get(0),
                                this.negotiationData);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write negotiation data to disk", e);
                    }
                }

                // Log the final outcome and terminate
                getReporter().log(Level.INFO, "Final outcome:" + info);
                terminate();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle info", e);
        }
    }

    /** Let GeniusWeb know what protocols that agent is capable of handling */
    @Override
    public Capabilities getCapabilities() {
        return new Capabilities(new HashSet<>(Arrays.asList("SAOP", "Learn")), Collections.singleton(Profile.class));
    }

    /** Terminate agent */
    @Override
    public void terminate() {
        super.terminate();
        if (this.profileint != null) {
            this.profileint.close();
            this.profileint = null;
        }
    }

    /*
     * *****************************NOTE:************************************
     * Everything below this comment is most relevant for the ANL competition.
     * **********************************************************************
     */

    /** Provide a description of the agent */
    @Override
    public String getDescription() {
        return "This is our model Olu-Sorin-3000.";
    }

    /**
     * Processes an Action performed by the opponent.
     * 
     * @param action
     */
    private void processAction(Action action) {
        if (action instanceof Offer) {
            // If the action was an offer: Obtain the bid and add it's value to our
            // negotiation data.
            this.lastReceivedBid = ((Offer) action).getBid();
            this.negotiationData.addBidUtil(this.uSpace.getUtility(this.lastReceivedBid).doubleValue());
            this.MCTS.receiveRealObservation(action, System.currentTimeMillis());
        }
    }

    /**
     * This method is called when the negotiation has finished. It can process the
     * final agreement.
     * 
     * @param agreements
     */
    private void processAgreements(Agreements agreements) {
        // Check if we reached an agreement (walking away or passing the deadline
        // results in no agreement)
        if (!agreements.getMap().isEmpty()) {
            // Get the bid that is agreed upon and add it's value to our negotiation data
            Bid agreement = agreements.getMap().values().iterator().next();
            System.out.println("AGREEMENT!!!! -- Util=" + String.valueOf(this.uSpace.getUtility(agreement)) + " -- "
                    + agreement.toString());
            this.negotiationData.addAgreementUtil(this.uSpace.getUtility(agreement).doubleValue());
        }
    }

    /**
     * send our next offer
     * 
     * @throws StateRepresentationException
     */
    private void myTurn() throws IOException, StateRepresentationException {
        if (this.lastReceivedBid != null) {
            if (DEBUG_IN_TOURNAMENT == false) {
                System.out.println("blatag: " + progress.get(System.currentTimeMillis()));
                System.out.println("Opponent: Util=" + this.uSpace.getUtility(this.lastReceivedBid) + " -- "
                        + this.lastReceivedBid.toString());
            }
        }
        Action action;

        if (isGood(this.lastReceivedBid)) {
            // If the last received bid is good: create Accept action
            action = new Accept(me, this.lastReceivedBid);
        } else {
            // STEP: Generate offer!
            long negotiationEnd = this.progress.getTerminationTime().getTime();
            long remainingTime = negotiationEnd - System.currentTimeMillis();
            long simTime = Math.min(SIMULATION_TIME, remainingTime);

            if (simTime <= 250) {
                this.MCTS.construct(simTime);
            }
            action = this.MCTS.chooseBestAction();
            if (action == null) {
                // if action==null we failed to suggest next action.
                System.err.println("WARNING! Could not produce action!!!");
                action = new Accept(this.me, lastReceivedBid);
            }
            if (action instanceof Offer) {
                Bid myBid = ((Offer) action).getBid();
                if (DEBUG_IN_TOURNAMENT == false) {
                    System.out.println("Agent:    Util=" + String.valueOf(this.uSpace.getUtility(myBid)) + " -- "
                            + myBid.toString());
                }
            } else if (action instanceof Accept) {
                Bid acceptedBid = ((Accept) action).getBid();
                System.out.println("We ACCEPT: Util=" + String.valueOf(this.uSpace.getUtility(acceptedBid)) + " -- "
                        + acceptedBid.toString());
                // if(this.lastReceivedBid.equals(acceptedBid) == false){
                // System.out.println("BUT needs to be placed as Offer!");
                // action = new Offer(this.me, acceptedBid);
                // }
            } else {
                System.out.println("Something HAPPENED! " + action.toString());
            }
            if (DEBUG) {
                System.out.println(this.MCTS);
                System.out.println(action);
            }
        }

        // Send action
        getConnection().send(action);
    }

    /**
     * The method checks if a bid is good.
     * 
     * @param bid the bid to check
     * @return true iff bid is good for us.
     */
    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;

        // Check if we already know the opponent
        if (this.persistentState.knownOpponent(this.opponentName)) {
            // Obtain the average of the max utility that the opponent has offered us in
            // previous negotiations.
            Double avgMaxUtility = this.persistentState.getAvgMaxUtility(this.opponentName);

            // Request 5% more than the average max utility offered by the opponent.
            return this.uSpace.getUtility(bid).doubleValue() > (avgMaxUtility * 1.05);
        }

        // Check a simple business rule
        Boolean nearDeadline = progress.get(System.currentTimeMillis()) > 0.95;
        Boolean acceptable = this.uSpace.getUtility(bid).doubleValue() > 0.7;
        Boolean good = this.uSpace.getUtility(bid).doubleValue() > 0.9;
        return (nearDeadline && acceptable) || good;
    }

    /**
     * This method is invoked if the learning phase is started. There is now time to
     * process previously stored data and use it to update our persistent state.
     * This persistent state is passed to the agent again in future negotiation
     * session. REMEMBER that there is a deadline of 60 seconds for this step.
     */
    private void learn() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Iterate through the negotiation data file paths
        for (File dataPath : this.dataPaths)
            try {
                // Load the negotiation data object of a previous negotiation
                NegotiationData negotiationData = objectMapper.readValue(dataPath, NegotiationData.class);

                // Process the negotiation data in our persistent state
                this.persistentState.update(negotiationData);
            } catch (IOException e) {
                throw new RuntimeException("Negotiation data provided to learning step does not exist", e);
            }

        // Write the persistent state object to file
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(this.persistentPath, this.persistentState);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write persistent state to disk", e);
        }
    }
}
