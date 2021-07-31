package geniusweb.custom.agent; // TODO: change name

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.websocket.DeploymentException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.FileLocation;
import geniusweb.actions.LearningDone;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.custom.components.Tree;
import geniusweb.custom.helper.Configurator;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.opponents.AntagonisticOpponentPolicy;
import geniusweb.custom.opponents.BoulwareOpponentPolicy;
import geniusweb.custom.opponents.ConcederOpponentPolicy;
import geniusweb.custom.opponents.HardLinerOpponentPolicy;
import geniusweb.custom.opponents.SelfishOpponentPolicy;
import geniusweb.custom.opponents.TimeDependentOpponentPolicy;
import geniusweb.custom.state.StateRepresentationException;
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

public class CustomAgent extends DefaultParty { // TODO: change name

    /**
     *
     */
    // private static final int NUM_SIMULATIONS = 100;
    // private static final int MAX_WIDTH = 10;
    private Long simulationTime = 500l; // TODO: BUG if increased
    private static final boolean DEBUG_LEARN = true;
    private static boolean DEBUG_OFFER = true;
    private static boolean DEBUG_SAVE_TREE = true;
    private static boolean DEBUG_IN_TOURNAMENT = false;
    private Bid lastReceivedBid = null;
    private PartyId me;
    protected ProfileInterface profileint = null;
    private Progress progress;
    private String protocol;
    private Parameters parameters;
    private UtilitySpace uSpace;
    private PersistentState persistentState;
    private NegotiationData negotiationData;
    private List<File> dataPaths = new ArrayList<>();
    private File persistentPath;
    private String opponentName;
    private Tree MCTS;
    private ObjectMapper mapper = new ObjectMapper();
    private Long numOpponentCopies = 10l;
    private Boolean isLearn = false;
    private HashMap<String, Object> config;

    public CustomAgent() {
    } // TODO: change name

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
                runSetupPhase(info);
            } else if (info instanceof ActionDone) {
                runOpponentPhase(info);
            } else if (info instanceof YourTurn) {
                runAgentPhase(info);
            } else if (info instanceof Finished) {
                runEndPhase(info);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to handle info", e);
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
                .setRealHistory(this.MCTS.getRealHistory());
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile, this.negotiationData);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write negotiation data to disk", e);
            }
        }
        if (DEBUG_SAVE_TREE) {
            saveTreeToLogs("full_".concat(sessionName), this.MCTS.toStringOriginal());
            saveTreeToLogs("curr_".concat(sessionName), this.MCTS.toString());
        }

        // Log the final outcome and terminate
        getReporter().log(Level.INFO, "Final outcome:" + info);
        terminate();
    }

    private void saveTreeToLogs(String fileName, String content) throws IOException {
        FileWriter fullTreeFileWriter = new FileWriter("logs/log_" + fileName + ".txt");
        fullTreeFileWriter.write(content);
        fullTreeFileWriter.close();
    }

    private void runAgentPhase(Inform info) throws IOException, StateRepresentationException {
        // Advance the round number if a round-based deadline is set.
        if (progress instanceof ProgressRounds) {
            progress = ((ProgressRounds) progress).advance();
        }
        if (info instanceof YourTurn) {
            // The info notifies us that it is our turn
            YourTurn myTurnInfo = (YourTurn) info;
            myTurn(myTurnInfo);
        }
    }

    private void runOpponentPhase(Inform info) {
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
                getReporter().log(Level.INFO, "VS. " + this.opponentName);
            }
            // Process the action of the opponent.
            processAction(action);
        }
    }

    private void runSetupPhase(Inform info)
            throws IOException, JsonParseException, JsonMappingException, DeploymentException, Exception {
        // info is a Settings object that is passed at the start of a negotiation
        Settings settings = (Settings) info;
        // Needs to run
        this.initializeVariables(settings);

        if (this.isLearn) {
            // We are in the learning step: We execute the learning and notify when we are
            // done. REMEMBER that there is a deadline of 60 seconds for this step.
            this.runLearnPhase(info);
        } else {
            this.initializeTree(settings);
        }
    }

    private void initializeTree(Settings settings) throws DeploymentException, Exception {
        // We are in the negotiation step.

        // Create a new NegotiationData object to store information on this negotiation.
        // See 'NegotiationData.java'.
        this.negotiationData = new NegotiationData().setConfiguration(this.config);
        // Obtain our utility space, i.e. the problem we are negotiating and our
        // preferences over it.
        try {
            this.profileint = ProfileConnectionFactory.create(settings.getProfile().getURI(), getReporter());
            this.uSpace = ((UtilitySpace) profileint.getProfile());
            // Our stuff

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (this.persistentState.getCurrentBelief() == null) {
            Domain domain = this.uSpace.getDomain();
            AllBidsList bidspace = new AllBidsList(domain);
            List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();

            for (int cnt = 0; cnt < this.numOpponentCopies; cnt++) {
                listOfOpponents.add(new AntagonisticOpponentPolicy(this.uSpace));
                listOfOpponents.add(new SelfishOpponentPolicy(domain));
                listOfOpponents.add(new TimeDependentOpponentPolicy(domain));
                listOfOpponents.add(new HardLinerOpponentPolicy(domain));
                listOfOpponents.add(new ConcederOpponentPolicy(domain));
                listOfOpponents.add(new BoulwareOpponentPolicy(domain));
            }
            listOfOpponents = listOfOpponents.stream().map(opponent -> opponent.setBidspace(bidspace))
                    .collect(Collectors.toList());

            Configurator configurator = this.config != null ? this.mapper.convertValue(config, Configurator.class)
                    : new Configurator();
            configurator = configurator.setUtilitySpace(this.uSpace).setListOfOpponents(listOfOpponents).setMe(this.me)
                    .build();

            this.MCTS = new Tree(this.uSpace, configurator.getBelief(), configurator.getInitState(),
                    configurator.getWidener(), this.progress);
        } else {
            this.MCTS = this.persistentState.reconstructTree(this.me, this.uSpace, this.progress);
        }

        if (this.MCTS == null) {
            throw new Exception("Failed to instantiate the tree.");
        }
    }

    private void initializeVariables(Settings settings) throws IOException, JsonParseException, JsonMappingException {
        // Protocol that is initiate for the agent
        this.protocol = settings.getProtocol().getURI().getPath();
        this.getReporter().log(Level.INFO, protocol);

        // ID of my agent
        this.me = settings.getID();

        // The progress object keeps track of the deadline
        this.progress = settings.getProgress();

        this.isLearn = "Learn".equals(protocol);

        // Parameters for the agent (can be passed through the GeniusWeb GUI, or a
        // JSON-file)
        this.parameters = settings.getParameters();
        if (this.parameters.containsKey("simulationTime")) {
            this.simulationTime = ((Number) this.parameters.get("simulationTime")).longValue();
        }
        if (this.parameters.containsKey("numParticlesPerOpponent")) {
            this.numOpponentCopies = ((Number) this.parameters.get("numParticlesPerOpponent")).longValue();
        }

        if (this.parameters.containsKey("config")) {
            this.config = (HashMap<String, Object>) this.parameters.get("config");
        } else {
            this.config = Configurator.generateDefaultConfig();
        }

        // The PersistentState is loaded here (see 'PersistenData,java')
        if (this.parameters.containsKey("persistentstate")) {
            UUID fileLocation = UUID.fromString((String) this.parameters.get("persistentstate"));
            this.persistentPath = new FileLocation(fileLocation).getFile();
        }
        if (this.persistentPath != null && this.persistentPath.exists()) {
            ObjectMapper objectMapper = new ObjectMapper();
            this.persistentState = objectMapper.readValue(this.persistentPath, PersistentState.class);
        } else {
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
        this.MCTS = null;
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
    protected void processAction(Action action) {
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
    protected void processAgreements(Agreements agreements) {
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
     * @param myTurnInfo
     *
     * @throws StateRepresentationException
     */
    protected void myTurn(YourTurn myTurnInfo) throws IOException, StateRepresentationException {
        if (this.lastReceivedBid != null) {
            if (DEBUG_IN_TOURNAMENT == false) {
                System.out.println("blatag: " + progress.get(System.currentTimeMillis()));
                System.out.println("Opponent: Util=" + this.uSpace.getUtility(this.lastReceivedBid) + " -- "
                        + this.lastReceivedBid.toString());
            }
        }
        Action action;
        
        // STEP: Generate offer!
        long negotiationEnd = this.progress.getTerminationTime().getTime();
        long remainingTime = negotiationEnd - System.currentTimeMillis();

        long simTime = this.simulationTime;
        // System.out.println(simTime <= remainingTime);
        if (simTime <= remainingTime) {
            this.MCTS.construct(simTime);
            // System.out.println(this.MCTS.toString());
            // DONE: Number of nodes do increase at each tree construction
            // System.out.println("Nodes Number: ".concat(String.valueOf(this.MCTS.howManyNodes())));
        }
        // TODO: consuming the whole tree will result in an error
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
        if (DEBUG_OFFER) {
            // System.out.println(this.MCTS);
            System.out.println(action);
        }

        // Send action
        getConnection().send(action);
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
        if (this.dataPaths.size() >= 0) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(this.persistentPath, this.persistentState);
                this.getConnection().send(new LearningDone(me));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write persistent state to disk", e);
            }
        }
    }
}
