package geniusweb.pompfan.agent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

import javax.websocket.DeploymentException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.FileLocation;
import geniusweb.actions.LearningDone;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.inform.ActionDone;
import geniusweb.inform.Agreements;
import geniusweb.inform.Finished;
import geniusweb.inform.Inform;
import geniusweb.inform.Settings;
import geniusweb.inform.YourTurn;
import geniusweb.issuevalue.Bid;
import geniusweb.party.Capabilities;
import geniusweb.party.DefaultParty;
import geniusweb.pompfan.components.ActionNode;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Configurator;
import geniusweb.pompfan.components.OpponentParticleCreator;
import geniusweb.pompfan.components.OpponentParticleCreatorHardcoded;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.state.StateRepresentationException;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import tudelft.utilities.immutablelist.ImmutableList;
import tudelft.utilities.logging.Reporter;

public class POMPFANAgent extends DefaultParty {

    /**
     *
     */
    private Long simulationTime = 500l;
    private static final boolean DEBUG_LEARN = false;
    private static final boolean DEBUG_OFFER = false;
    private static final boolean DEBUG_PERSIST = false;
    private static final boolean DEBUG_SAVE_TREE = false;
    // turn on for state estimation experiments
    private static final boolean DEBUG_BELIEF = false;
    private static final boolean DEBUG_TIME = false;
    private Bid lastReceivedBid = null;
    private PartyId me;
    private PartyId opp;
    protected ProfileInterface profileint = null;
    private Progress progress;
    private String protocol;
    private Parameters parameters;
    private UtilitySpace uSpace;
    private PersistentState persistentState;
    private NegotiationData negotiationData;
    private List<Action> oppActions = new ArrayList<Action>();
    private List<File> dataPaths = new ArrayList<>();
    private File persistentPath;
    private String opponentName;
    private Tree MCTS;
    private ObjectMapper mapper = new ObjectMapper();
    private Long numParticlesPerOpponent = 10l;
    private Boolean isLearn = false;
    private HashMap<String, Object> config;
    private BidsWithUtility bidsWithUtility;
    private ImmutableList<Bid> goodBids;
    private Random random = new Random();
    private Double dataCollectionTime = 0.25;

    public POMPFANAgent() {
    }

    public POMPFANAgent(Reporter reporter) {
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
        try {
            if (info instanceof Settings) {
                if (DEBUG_TIME)
                    this.me = ((Settings) info).getID();
                if (DEBUG_TIME)
                    this.progress = ((Settings) info).getProgress();
                if (DEBUG_TIME)
                    System.out.println(this.me.getName() + ": Setup");
                runSetupPhase(info);
            } else if (info instanceof ActionDone) {
                // System.out.println("INFO= " + info);
                if (DEBUG_TIME)
                    System.out.println(this.me.getName() + " - " + this.progress.get(System.currentTimeMillis())
                            + ": ActionDone - " + ((ActionDone) info).getAction().getActor());
                runOpponentPhase(info);

            } else if (info instanceof YourTurn) {
                if (DEBUG_TIME)
                    System.out.println(
                            this.me.getName() + " - " + this.progress.get(System.currentTimeMillis()) + ": YourTurn");
                runAgentPhase(info);
            } else if (info instanceof Finished) {
                System.out.println("INFO= " + info);
                if (DEBUG_TIME)
                    System.out.println(
                            this.me.getName() + " - " + this.progress.get(System.currentTimeMillis()) + ": Finished");
                runEndPhase(info);
            }
            if (DEBUG_TIME)
                System.out.println(this.me.getName() + " - " + this.progress.get(System.currentTimeMillis())
                        + ": END Cycle - " + info.getClass().getSimpleName());
            cleanupIfGameOver();
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle info", e);
        }
    }

    private void cleanupIfGameOver() throws IOException{
        if (this.progress.isPastDeadline(System.currentTimeMillis())) {
            getReporter().log(Level.INFO, "Game's over!");
            FileWriter finalLogWriter = new FileWriter("eval/result_" + "last_experiment" + ".jsonl", true);
            String content = this.me.getName() + "," + this.opponentName + "," + 0.0 + "," + "Bid{}" + "," + "Bid{}" +"\n";
            finalLogWriter.write(content);
            finalLogWriter.close();
            terminate();
        }
    }

    private void runSetupPhase(Inform info)
            throws IOException, JsonParseException, JsonMappingException, DeploymentException, Exception {

        // info is a Settings object that is passed at the start of a negotiation
        Settings settings = (Settings) info;
        // Needs to run
        if (DEBUG_LEARN)
            System.out.println("DEBUG_LEARN_PERSISTENCE: ========================================= "
                    .concat(settings.getID().toString()));
        if (DEBUG_LEARN)
            System.out.println("DEBUG_LEARN_PERSISTENCE: ".concat(settings.toString()));
        this.initializeVariables(settings);

        if (this.isLearn) {
            // We are in the learning step: We execute the learning and notify when we are
            // done. REMEMBER that there is a deadline of 60 seconds for this step.
            if (DEBUG_LEARN)
                System.out.println("DEBUG_LEARN_PERSISTENCE: Enter learn");
            this.runLearnPhase(info);
        } else {
            if (DEBUG_LEARN)
                System.out.println("DEBUG_LEARN_PERSISTENCE: Enter tree init");
            this.initializeTree(settings);
            this.goodBids = this.bidsWithUtility.getBids(new Interval(new BigDecimal(0.8), BigDecimal.ONE));
        }

        if (DEBUG_BELIEF) {
            String contentDetailed = this.MCTS.getBelief().toDetailedString();
            String content = this.MCTS.getBelief().toCoarseString();
            saveDistributionToLogs(
                    "distribution_detailed", "{\"Distance\": \"" + this.MCTS.getBelief().getDistance().getClass().getSimpleName() + "\"}", false);
            saveDistributionToLogs("distribution_detailed", contentDetailed, true);
            saveDistributionToLogs("distribution", content, false);
        }
    }

    private void runOpponentPhase(Inform info) throws IOException {
        // The info object is an action that is performed by an agent.
        Action action = ((ActionDone) info).getAction();

        // ActionDone could also be our action, which is stupid
        // Check if this is not our own action
        if (!this.me.equals(action.getActor())) {
            // Check if we already know who we are playing against.
            if (this.opponentName == null) {
                // The part behind the last _ is always changing, so we must cut it off.
                this.opp = action.getActor();
                String fullOpponentName = action.getActor().getName();
                int lastIndexOf = fullOpponentName.lastIndexOf("_");
                int index = lastIndexOf;
                this.opponentName = fullOpponentName.substring(0, index);

                // Add name of the opponent to the negotiation data
                this.negotiationData.setOpponentName(this.opponentName);
                if (this.persistentState.getAllOpponentBeliefs().containsKey(this.opponentName)) {
                    if (DEBUG_PERSIST)
                        System.out.println("DEBUG_PERSIST: Load -> " + this.me);
                    if (DEBUG_PERSIST)
                        System.out.println("DEBUG_PERSIST: Load VS ->" + this.opponentName);
                    if (DEBUG_PERSIST)
                        System.out.println(
                                "DEBUG_PERSIST: Load ->" + this.persistentState.getAllOpponentBeliefs().toString());
                    this.MCTS = this.persistentState.reconstructTree(this.me, this.uSpace, this.progress,
                            this.opponentName, this.numParticlesPerOpponent);
                    if (DEBUG_PERSIST)
                        System.out.println("DEBUG_PERSIST: Trust->" + this.MCTS.getBelief().toString());

                }

            }

            processAction(action);
            // getConnection().send(null);
        }
    }

    private void runAgentPhase(Inform info) throws IOException, StateRepresentationException {
        // Advance the round number if a round-based deadline is set.

        if (progress instanceof ProgressRounds) {
            progress = ((ProgressRounds) progress).advance();
        }
        if (info instanceof YourTurn) {
            try {
                // The info notifies us that it is our turn
                YourTurn myTurnInfo = (YourTurn) info;
                ActionWithBid action = (ActionWithBid) myTurn(myTurnInfo);
                this.MCTS.getRealHistory().add(action);
                if (DEBUG_OFFER == true) {
                    System.out.println("Current Time: " + progress.get(System.currentTimeMillis()));
                    System.out.println("Agent: Util=" + this.uSpace.getUtility(action.getBid()) + " -- "
                            + action.getBid().toString());
                }
                getConnection().send(action);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getClass());
                this.getReporter().log(Level.WARNING, "Problem in YourTurn!!!");
                if (DEBUG_OFFER == true) {
                    System.out.println("Current Time: " + progress.get(System.currentTimeMillis()));
                    System.out.println("Agent: Util= " + "Some ERR");
                }
                getConnection().send(new Accept(this.me, this.lastReceivedBid));
            }
        }
    }

    private void runEndPhase(Inform info) throws IOException {
        // The info is a notification that th negotiation has ended. This Finished
        // object also contains the final agreement (if any).
        Agreements agreements = ((Finished) info).getAgreement();
        processAgreements(agreements);
        String sessionName = "";
        // Write the negotiation data that we collected to the path provided.
        if (this.negotiationData != null && !this.dataPaths.isEmpty()) {
            File sessionFile = this.dataPaths.get(0);
            sessionName = sessionFile.getName();
            try {
                this.negotiationData.setBelief(this.MCTS.getBelief()).setRoot(this.MCTS.getRoot())
                        .setRealOppHistory(this.oppActions);

                if (DEBUG_PERSIST)
                    System.out.println("DEBUG_PERSIST: End -> " + this.me);
                if (DEBUG_PERSIST)
                    System.out.println("DEBUG_PERSIST: End -> " + this.negotiationData.getBelief());

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile, this.negotiationData);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write negotiation data to disk", e);
            }
        }

        if (DEBUG_SAVE_TREE) {
            System.out.println("INSIDE: Saving the tree.");
            saveTreeToLogs("tree_".concat(sessionName), this.MCTS.toString());
        }

        // Log the final outcome and terminate
        if (agreements.getMap().size() > 0) {

            // getReporter().log(Level.INFO, "Final outcome: " + this.me.getName() + ": "
            //         + this.uSpace.getUtility(agreements.getMap().get(this.me)) + " " + info);
            
            // TODO: get the utility of the opponent somehow
            // TODO: see if can get other metrics
            // The only thing that should not be complicated to just get metrics in the end...
            FileWriter finalLogWriter = new FileWriter("eval/result_" + "last_experiment" + ".jsonl", true);
            String content = this.me.getName() + ","
                    + this.opponentName + "," + this.uSpace.getUtility(agreements.getMap().get(this.me)) + ","
                    + agreements.getMap().get(this.me) + "," + agreements.getMap().get(this.opp) + "\n";
            finalLogWriter.write(content);
            finalLogWriter.close();

        }
        terminate();
    }

    private void saveTreeToLogs(String fileName, String content) throws IOException {
        FileWriter fullTreeFileWriter = new FileWriter("logs/log_" + fileName + ".txt");
        fullTreeFileWriter.write(content);
        fullTreeFileWriter.close();
    }

    private void saveDistributionToLogs(String fileName, String content, Boolean isAppend) throws IOException {
        FileWriter fullTreeFileWriter = new FileWriter("logs/log_" + fileName + ".jsonl", isAppend);
        fullTreeFileWriter.write(content + "\n");
        fullTreeFileWriter.close();
    }

    private void initializeTree(Settings settings) throws DeploymentException, Exception {
        // We are in the negotiation step.

        // Create a new NegotiationData object to store information on this negotiation.
        // See 'NegotiationData.java'.
        this.negotiationData = new NegotiationData().setConfiguration(this.config);
        // Obtain our utility space, i.e. the problem we are negotiating and our
        // preferences over it.
        try {
            // Our stuff
            this.profileint = ProfileConnectionFactory.create(settings.getProfile().getURI(), getReporter());
            this.uSpace = ((UtilitySpace) profileint.getProfile());
            this.bidsWithUtility = new BidsWithUtility((LinearAdditive) this.uSpace);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        if (DEBUG_PERSIST)
            System.out.println("DEBUG_PERSIST: Opp -> " + this.opponentName);

        // List<AbstractPolicy> listOfOpponents = OpponentParticleCreator.generateOpponentParticles(this.uSpace,
        //         this.numParticlesPerOpponent, this.progress);
        // Experiment1
        List<AbstractPolicy> listOfOpponents = OpponentParticleCreatorHardcoded.generateOpponentParticles(this.uSpace,
                this.numParticlesPerOpponent, this.progress);

        Configurator configurator = this.config != null ? this.mapper.convertValue(config, Configurator.class)
                : new Configurator();
        configurator = configurator.setUtilitySpace(this.uSpace).setListOfOpponents(listOfOpponents).setMe(this.me)
                .build();
        
        if (DEBUG_PERSIST)
            System.out.println("DEBUG_PERSIST: Init -> " + this.me);
        if (DEBUG_PERSIST)
            System.out.println("DEBUG_PERSIST: Init -> " + configurator.getBelief().toString());
        
        // the creation of a monster
        this.MCTS = new Tree(this.uSpace, configurator.getBelief(), configurator.getInitState(),
                configurator.getWidener(), this.progress);

        if (this.MCTS == null) {
            throw new Exception("Failed to instantiate the tree.");
        }
    }

    private void initializeVariables(Settings settings) throws IOException, JsonParseException, JsonMappingException {
        // Protocol that is initiate for the agent
        this.protocol = settings.getProtocol().getURI().getPath();
        // this.getReporter().log(Level.INFO, protocol);

        // ID of my agent
        this.me = settings.getID();

        // The progress object keeps track of the deadline
        this.progress = settings.getProgress();

        this.isLearn = "Learn".equals(protocol);
        // Parameters for the agent (can be passed through the GeniusWeb GUI, or a
        // JSON-file)
        this.parameters = settings.getParameters();
        if (this.parameters.containsKey("simulationTime")) {
            this.simulationTime = Long.valueOf(((String) this.parameters.get("simulationTime")));
        }
        if (this.parameters.containsKey("numParticlesPerOpponent")) {
            this.numParticlesPerOpponent = Long.valueOf(((String) this.parameters.get("numParticlesPerOpponent")));
        }
        if (this.parameters.containsKey("dataCollectionTime")) {
            this.dataCollectionTime = Double.valueOf(((String) this.parameters.get("dataCollectionTime")));
        }

        if (this.parameters.containsKey("config")) {
            this.config = (HashMap<String, Object>) this.parameters.get("config");
        } else {
            this.config = Configurator.generateDefaultConfig();
        }

        // The PersistentState is loaded here (see 'PersistenData.java')
        if (this.parameters.containsKey("persistentstate")) {
            if (DEBUG_LEARN)
                System.out.println("DEBUG_LEARN_PERSISTENCE: Found Persistence!");
            UUID fileLocation = UUID.fromString((String) this.parameters.get("persistentstate"));
            this.persistentPath = new FileLocation(fileLocation).getFile();
        }
        if (this.persistentPath != null && this.persistentPath.exists()) {
            if (DEBUG_LEARN)
                System.out.println("DEBUG_LEARN_PERSISTENCE: Load Persistence!");
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                this.persistentState = objectMapper.readValue(this.persistentPath, PersistentState.class);
            } catch (Exception e) {
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.protocol);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.progress);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.progress.get(System.currentTimeMillis()));
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.config);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.dataPaths);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.isLearn);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.me);
                e.printStackTrace();
                throw e;
            }
        } else {
            if (DEBUG_LEARN)
                System.out.println("DEBUG_LEARN_PERSISTENCE: Create Persistence!");
            this.persistentState = new PersistentState();
        }

        // The negotiation data paths are converted here from List<String> to List<File>
        // for improved usage. For safety reasons, this is more comprehensive than
        // normally.
        if (DEBUG_LEARN) {
            System.out.println("DEBUG_LEARN: Protocol " + protocol);
            System.out.println("DEBUG_LEARN: Persistent-Data:  " + this.parameters.get("persistentstate").toString());

            if (this.parameters.containsKey("negotiationdata")) {
                System.out.println("DEBUG_LEARN: Nego-Data: " + this.parameters.get("negotiationdata").toString());
            }
        }

        if (this.parameters.containsKey("negotiationdata")) {
            this.dataPaths = new ArrayList<>();
            List<Object> dataPaths_raw = this.parameters.get("negotiationdata", List.class);
            for (Object path : (List<Object>) dataPaths_raw) {
                FileLocation tmpPath = path instanceof FileLocation ? (FileLocation) path
                        : new FileLocation(UUID.fromString((String) path));
                this.dataPaths.add(tmpPath.getFile());
            }
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
        System.out.println("POMPFAN closed");
        // super.terminate();
        // if (this.profileint != null) {
        //     this.profileint.close();
        //     this.profileint = null;
        // }
        // this.oppActions = null;
        // this.MCTS = null;
        // this.uSpace = null;
        // this.config = null;
        // this.bidsWithUtility = null;
        // this.negotiationData = null;
        // this.persistentState = null;
        // this.parameters = null;
        // this.mapper = null;
    }

    /*
     * *****************************NOTE:************************************
     * Everything below this comment is most relevant for the ANL competition.
     * **********************************************************************
     */

    /** Provide a description of the agent */
    @Override
    public String getDescription() {
        return "Partially Observable MCTS using Particle Filter belief for Automated Negotiation.";
    }

    /**
     * Processes an Action performed by the opponent.
     *
     * @param action
     * @throws IOException
     */
    protected void processAction(Action action) throws IOException {
        // Process the action of the opponent.
        this.MCTS.getRealHistory().add(action);
        this.oppActions.add(action);

        if (action instanceof Offer) {
            // If the action was an offer: Obtain the bid and add it's value to our
            // negotiation data.
            this.lastReceivedBid = ((Offer) action).getBid();
            this.negotiationData.addBidUtil(this.uSpace.getUtility(this.lastReceivedBid).doubleValue());
            this.MCTS.receiveRealObservation(action, System.currentTimeMillis());
            
            if (DEBUG_BELIEF) {
                String contentDetailed = this.MCTS.getBelief().toDetailedString();
                String content = this.MCTS.getBelief().toString();
                // String diferentiator = Long.toString(System.currentTimeMillis());
                
                saveDistributionToLogs("distribution_detailed", contentDetailed, true);
                saveDistributionToLogs("distribution", content, true);
            }
        }
        
        if (DEBUG_OFFER) {
            ActionWithBid aBid = (ActionWithBid) action;
            System.out.println("Current Time: " + progress.get(System.currentTimeMillis()));
            System.out.println(
                    "Counteroffer: Util=" + this.uSpace.getUtility(aBid.getBid()) + " -- " + aBid.getBid().toString());
        }
    }

    /**
     * This method is called when the negotiation has finished. It can process the
     * final agreement.
     *
     * @param agreements
     */
    protected void processAgreements(Agreements agreements) {
        // Check if we reached an agreement (walking away or passing the deadline
        // results in no agreement)
        if (!agreements.getMap().isEmpty()) {
            // Get the bid that is agreed upon and add it's value to our negotiation data
            Bid agreement = agreements.getMap().values().iterator().next();
            System.out.println("AGREEMENT!!!! -- Util=" + String.valueOf(this.uSpace.getUtility(agreement)) + " -- "
                    + agreement.toString());
            this.negotiationData.addAgreementUtil(this.uSpace.getUtility(agreement).doubleValue());
        } else {
            System.out.println("NO AGREEMENT!!!! ");
        }

    }

    /**
     * send our next offer
     *
     * @param myTurnInfo
     *
     * @throws StateRepresentationException
     */
    protected Action myTurn(YourTurn myTurnInfo) throws IOException, StateRepresentationException {
        Action action;
        Bid bid;
        long negotiationEnd = this.progress.getTerminationTime().getTime();
        long simTime = this.simulationTime;
        if (this.progress.get(System.currentTimeMillis()) < this.dataCollectionTime) {
            // ?? For some reason this part fills the RAM quickly
            // High throughput bidding used for data collection
            bid = this.goodBids.get(this.random.nextInt(this.goodBids.size().intValue()));
            action = new Offer(this.me, bid);
            return action;
        }

        if (this.opponentName == null) {
            // The first one to make the offer
            bid = this.bidsWithUtility.getExtremeBid(true);
            action = new Offer(this.me, bid);
            return action;
        }

        long remainingTime = negotiationEnd - System.currentTimeMillis();
        // 2 * simTime (+ other execution delay) because we shift the progress in the simulation.
        if (1.5 * simTime < remainingTime) {
            // this.MCTS.scrapeSubTree();
            // System.gc();
            // When we start the tree construction we use the last real observation to guide the initial exploration
            // We do this just once at the very beginning after the data collection phase
            this.MCTS.getRoot().setObservation(this.MCTS.getRealHistory().get(this.MCTS.getRealHistory().size()-1));
            
            this.MCTS.construct(simTime, this.progress);
            
        } else {
            getReporter().log(Level.WARNING, "Not enough time! Start consuming the tree");
            // System.out.println(MCTS);
        }

        if (DEBUG_OFFER) {
            getReporter().log(Level.INFO, this.MCTS.getRoot().toString());
            // System.out.println(this.MCTS.toString());
            // getReporter().log(Level.INFO, "Tree has: " + String.valueOf(this.MCTS.howManyNodes()));
            getReporter().log(Level.INFO, "Tree root time was: " + this.MCTS.getRoot().getState().getTime());
        }

        action = this.MCTS.chooseBestAction();
      

        // Consuming the whole tree will result in an error
        // So accept
        // ? proposing old bids also possible
        if (action == null) {
            this.getReporter().log(Level.WARNING, "Could not produce new action!!!");
            ActionNode lastBestActionNode = this.MCTS.getLastBestActionNode();
            if (lastBestActionNode != null) {
                action = (ActionWithBid) lastBestActionNode.getAction();
                System.out.println(action);
                return action;
            }

            this.getReporter().log(Level.WARNING, "Could not get last best action node!!!");
            bid = this.bidsWithUtility.getExtremeBid(true);
            action = new Offer(this.me, bid);
            return action;
        }
        // Logging agent decisions
        if (action instanceof Accept) {
            Bid acceptedBid = ((Accept) action).getBid();
            if (DEBUG_OFFER == true)
                System.out.println("We ACCEPT: Util=" + String.valueOf(this.uSpace.getUtility(acceptedBid)) + " -- "
                        + acceptedBid.toString());
            return action;
        }

        if (action instanceof Offer) {
            Bid myBid = ((Offer) action).getBid();
            if (DEBUG_OFFER == true)
                System.out.println(
                        "Agent: Util=" + String.valueOf(this.uSpace.getUtility(myBid)) + " -- " + myBid.toString());
            return action;
        }
        this.getReporter().log(Level.SEVERE, "Something unexpected HAPPENED! " + action.toString());
        return action;
    }

    /**
     * This method is invoked if the learning phase is started. There is now time to
     * process previously stored data and use it to update our persistent state.
     * This persistent state is passed to the agent again in future negotiation
     * session. REMEMBER that there is a deadline of 60 seconds for this step.
     * 
     * @param info
     */
    protected void runLearnPhase(Inform info) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Iterate through the negotiation data file paths
        for (File dataPath : this.dataPaths) {
            if (!dataPath.exists()) {
                getReporter().log(Level.WARNING, "File: ".concat(dataPath.toString()).concat(" does not exist!"));
                continue;
            }
            try {
                // Load the negotiation data object of a previous negotiation
                NegotiationData negotiationData = objectMapper.readValue(dataPath, NegotiationData.class);
                // Process the negotiation data in our persistent state
                this.persistentState.update(negotiationData);
            } catch (IOException e) {
                throw new RuntimeException("Negotiation data provided to learning step does not exist", e);
            }
        }

        // Write the persistent state object to file
        if (this.dataPaths.size() > 0) {
            try {
                PersistentState updatedState = this.persistentState.learn();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(this.persistentPath, updatedState);
                this.getConnection().send(new LearningDone(me));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write persistent state to disk", e);
            }
        }
    }
}
