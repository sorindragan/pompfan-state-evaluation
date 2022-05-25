package geniusweb.opponents;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileConnectionFactory;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import geniusweb.progress.ProgressRounds;
import geniusweb.references.Parameters;
import javafx.util.Pair;
import tudelft.utilities.immutablelist.ImmutableList;
import tudelft.utilities.logging.Reporter;

public abstract class AbstractOpponent extends DefaultParty {
    private boolean isLearn;
    private BidsWithUtility bidsWithUtility;
    private HashMap<BigDecimal, ArrayList<Bid>> bidsHash = new HashMap<BigDecimal, ArrayList<Bid>>();
    private ArrayList<BigDecimal> sortedUtilKeys = new ArrayList<BigDecimal>();
    private ImmutableList<Bid> goodBids;
    private List<ActionWithBid> history = new ArrayList<ActionWithBid>();
    private PartyId me;
    private String opponentName;
    private NegotiationData negotiationData;
    private PersistentState persistentState;
    private UtilitySpace utilitySpace;
    private File persistentPath;
    private ObjectMapper mapper = new ObjectMapper();
    private Random random = new Random();
    private Progress progress;
    protected ProfileInterface profileint = null;
    private Bid lastReceivedBid;
    public Pair<Bid, BigDecimal> minBidWithUtil;
    public Pair<Bid, BigDecimal> maxBidWithUtil;
    private List<File> dataPaths = new ArrayList<>();
    private TreeMap<BigDecimal, Bid> searchTree = new TreeMap<>();
    protected boolean DEBUG_OFFER = false;
    // private T param;
    private Parameters parameters;
    private String protocol;
    protected boolean DEBUG_TIME;

    public AbstractOpponent() {}

    public AbstractOpponent(Reporter reporter) {
        super(reporter); // for debugging
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace uSpace) {
        this.utilitySpace = uSpace;
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
        // System.out.println("===========INFO========== " + info.getClass().getName());
        try {
            if (info instanceof Settings) {
                if (DEBUG_TIME)
                    this.me = ((Settings) info).getID();
                if (DEBUG_TIME)
                    this.progress = ((Settings) info).getProgress();
                if (DEBUG_TIME)
                    System.out.println(
                            this.me.getName() + " - " + this.progress.get(System.currentTimeMillis()) + ": Setup");
                runSetupPhase(info);
            } else if (info instanceof ActionDone) {
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
            e.printStackTrace();
            throw new RuntimeException("Failed to handle info", e);
        }
    }

    protected void runEndPhase(Inform info) throws IOException {
        Agreements agreements = ((Finished) info).getAgreement();
        if (!agreements.getMap().isEmpty()) {
            // Get the bid that is agreed upon and add it's value to our negotiation data
            Bid agreement = agreements.getMap().values().iterator().next();
            this.negotiationData.addAgreementUtil(this.utilitySpace.getUtility(agreement).doubleValue());
            processAgreements(agreements);
        } else {
            processNonAgreements(agreements);
        }
        // Write the negotiation data that we collected to the path provided.
        if (this.negotiationData != null && !this.dataPaths.isEmpty()) {
            File sessionFile = this.dataPaths.get(0);
            try {
                this.mapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile, this.negotiationData);
            } catch (IOException e) {
                // throw new RuntimeException("Failed to write negotiation data to disk", e);
                System.err.println("Failed to write negotiation data to disk" + e);
                System.exit(0);
            }
        }
        // Log the final outcome and terminate
        terminate();
    };

    private void cleanupIfGameOver() {
        if (this.progress.isPastDeadline(System.currentTimeMillis())) {
            getReporter().log(Level.INFO, "Game's over!");
            terminate();
        }
    }

    protected void runAgentPhase(Inform info) throws IOException {
        if (this.getProgress() instanceof ProgressRounds) {
            this.setProgress(((ProgressRounds) this.getProgress()).advance());
        }
        if (info instanceof YourTurn) {
            try {
                // The info notifies us that it is our turn
                ActionWithBid action = this.myTurn(null);
                if (DEBUG_OFFER == true) {
                    System.out.println("Current Time: " + this.getProgress().get(System.currentTimeMillis()));
                    System.out.println("Agent: Util=" + this.getUtilitySpace().getUtility(action.getBid()) + " -- "
                            + action.getBid().toString());
                }
                this.getHistory().add(action);
                getConnection().send(action);
            } catch (Exception e) {
                this.getReporter().log(Level.WARNING, "First level fallback even failed!!!");
                e.printStackTrace();
                if (DEBUG_OFFER == true) {
                    System.out.println("Current Time: " + this.getProgress().get(System.currentTimeMillis()));
                    System.out.println("Agent: Util= " + "Some ERR");
                }
                Accept action = new Accept(this.getMe(), this.getFallbackBid());
                this.getHistory().add(action);
                getConnection().send(action);
            }
        }
    }

    protected void runOpponentPhase(Inform info) throws IOException {
        // The info object is an action that is performed by an agent.
        Action action = ((ActionDone) info).getAction();
        // Check if this is not our own action
        if (!this.me.equals(action.getActor())) {
            if (action instanceof Offer) {
                // If the action was an offer: Obtain the bid and add it's value to our
                // negotiation data.
                this.lastReceivedBid = ((Offer) action).getBid();
                this.negotiationData.addBidUtil(this.utilitySpace.getUtility(this.lastReceivedBid).doubleValue());
            }
            if (action instanceof ActionWithBid) {
                // Check if we already know who we are playing against.
                if (this.opponentName == null) {
                    // The part behind the last _ is always changing, so we must cut it off.
                    String fullOpponentName = action.getActor().getName();
                    int lastIndexOf = fullOpponentName.lastIndexOf("_");
                    int index = lastIndexOf;
                    this.opponentName = fullOpponentName.substring(0, index);

                    // Add name of the opponent to the negotiation data
                    this.negotiationData.setOpponentName(this.opponentName);
                }

                ActionWithBid actionWithBid = (ActionWithBid) action;
                this.getHistory().add(actionWithBid);
                processOpponentAction(actionWithBid);
                // getConnection().send(null);
            }
        }
    }

    protected abstract void processOpponentAction(ActionWithBid action);

    protected void runLearnPhase(Inform info) throws IOException {
        // Iterate through the negotiation data file paths
        for (File dataPath : this.dataPaths) {
            if (!dataPath.exists()) {
                getReporter().log(Level.WARNING, "File: ".concat(dataPath.toString()).concat(" does not exist!"));
                continue;
            }
            try {
                // Load the negotiation data object of a previous negotiation
                NegotiationData negotiationData = this.mapper.readValue(dataPath, NegotiationData.class);
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
                this.mapper.writerWithDefaultPrettyPrinter().writeValue(this.persistentPath, updatedState);
                this.getConnection().send(new LearningDone(me));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write persistent state to disk", e);
            }
        }
        getConnection().send(new LearningDone(me));
    };

    protected void initializeVariables(Settings settings) throws DeploymentException, IOException {
        this.profileint = ProfileConnectionFactory.create(settings.getProfile().getURI(), getReporter());
        this.setUtilitySpace((UtilitySpace) this.profileint.getProfile());
        this.setExtendedspace(new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace()));
        this.bidsWithUtility = new BidsWithUtility((LinearAdditiveUtilitySpace) this.getUtilitySpace());
        // create utils to bids hashmap
        BigDecimal currUtility = BigDecimal.ZERO;
        ArrayList<Bid> currBidsWithUtil = new ArrayList<>();
        for (Bid bid: this.bidsWithUtility.getBids(new Interval(BigDecimal.ZERO, BigDecimal.ONE))) {
            currUtility = this.getUtilitySpace().getUtility(bid);
            if (this.bidsHash.containsKey(currUtility)) {
                currBidsWithUtil = this.bidsHash.get(currUtility);
                currBidsWithUtil.add(bid);
                this.bidsHash.put(currUtility, currBidsWithUtil);
                continue;
            }
            this.sortedUtilKeys.add(currUtility);
            currBidsWithUtil = (ArrayList<Bid>) Stream.of(bid).collect(Collectors.toList());
            this.bidsHash.put(currUtility, currBidsWithUtil);
        }
        this.sortedUtilKeys.sort(new Comparator<BigDecimal>() {
            @Override
            public int compare(BigDecimal o1, BigDecimal o2) {
                return o1.compareTo(o2);
            }
            });
        
        this.minBidWithUtil = new Pair<Bid, BigDecimal>(this.bidsHash.get(this.sortedUtilKeys.get(0)).get(0), this.sortedUtilKeys.get(0));
        this.maxBidWithUtil = new Pair<Bid, BigDecimal>(this.bidsHash.get(this.sortedUtilKeys.get(this.sortedUtilKeys.size()-1)).get(0),
                this.sortedUtilKeys.get(this.sortedUtilKeys.size() - 1));

        // SearchFunction test; also tested for 0.0 and 1.0
        // System.out.println(this.sortedUtilKeys);
        // Bid someBid = this.getBidWithUtility(new BigDecimal("0.5732"));
        // System.out.println("----------");
        // System.out.println(someBid);

        this.protocol = settings.getProtocol().getURI().getPath();
        this.me = settings.getID();
        this.progress = settings.getProgress();
        this.isLearn = "Learn".equals(protocol);
        this.parameters = settings.getParameters();
        this.negotiationData = new NegotiationData();
        if (this.persistentPath != null && this.persistentPath.exists()) {
            try {
                this.persistentState = mapper.readValue(this.persistentPath, PersistentState.class);
            } catch (Exception e) {
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.protocol);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.progress);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.progress.get(System.currentTimeMillis()));
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.dataPaths);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.isLearn);
                System.out.println("DEBUG_LEARN_PERSISTENCE: " + this.me);
                e.printStackTrace();
                throw e;
            }
        } else {
            this.persistentState = new PersistentState();
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
    };

    protected void runSetupPhase(Inform info)
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
            this.setGoodBids(this.bidsWithUtility.getBids(new Interval(new BigDecimal(0.8), BigDecimal.ONE)));
        }

    }

    public TreeMap<BigDecimal, Bid> getSearchTree() {
        return searchTree;
    }

    public void setSearchTree(TreeMap<BigDecimal, Bid> searchTree) {
        this.searchTree = searchTree;
    }

    public ImmutableList<Bid> getGoodBids() {
        return goodBids;
    }

    public void setGoodBids(ImmutableList<Bid> goodBids) {
        this.goodBids = goodBids;
    }

    public boolean isLearn() {
        return isLearn;
    }

    public void setLearn(boolean isLearn) {
        this.isLearn = isLearn;
    }

    public BidsWithUtility getBidsWithUtility() {
        return bidsWithUtility;
    }

    public void setBidsWithUtility(BidsWithUtility bidsWithUtility) {
        this.bidsWithUtility = bidsWithUtility;
    }

    public List<ActionWithBid> getHistory() {
        return history;
    }

    public List<ActionWithBid> getOwnHistory() {
        return history.stream().filter(action -> action.getActor().equals(this.getMe())).collect(Collectors.toList());
    }

    public List<ActionWithBid> getOpponentHistory() {
        List<ActionWithBid> oppHistory = history.stream().filter(action -> !action.getActor().equals(this.getMe()))
                .collect(Collectors.toList());
        return oppHistory;
    }

    public void setHistory(List<ActionWithBid> history) {
        this.history = history;
    }

    /** Provide a description of the agent */
    @Override
    public String getDescription() {
        return "Partially Observable MCTS using Particle Filter belief for Automated Negotiation.";
    }

    /** Let GeniusWeb know what protocols that agent is capable of handling */
    @Override
    public Capabilities getCapabilities() {
        return new Capabilities(new HashSet<>(Arrays.asList("SAOP", "Learn")), Collections.singleton(Profile.class));
    }

    protected Bid getFallbackBid() {
        List<ActionWithBid> tmp = this.getOpponentHistory();
        return tmp.size() > 0 ? tmp.get(tmp.size() - 1).getBid() : null;
    }

    public PartyId getMe() {
        return me;
    }

    public void setMe(PartyId me) {
        this.me = me;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
    }

    public NegotiationData getNegotiationData() {
        return negotiationData;
    }

    public void setNegotiationData(NegotiationData negotiationData) {
        this.negotiationData = negotiationData;
    }

    public PersistentState getPersistentState() {
        return persistentState;
    }

    public void setPersistentState(PersistentState persistentState) {
        this.persistentState = persistentState;
    }

    public File getPersistentPath() {
        return persistentPath;
    }

    public void setPersistentPath(File persistentPath) {
        this.persistentPath = persistentPath;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Progress getProgress() {
        return progress;
    }

    public void setProgress(Progress progress) {
        this.progress = progress;
    }

    /** Terminate agent */
    @Override
    public void terminate() {
        super.terminate();
        if (this.profileint != null) {
            this.profileint.close();
            this.profileint = null;
        }
        System.out.println("Opponent closed");
        // super.terminate();
        // if (this.profileint != null) {
        //     this.profileint.close();
        //     this.profileint = null;
        // }
        // this.dataPaths = null;
        // this.goodBids = null;
        // this.history = null;
        // this.utilitySpace = null;
        // this.mapper = null;
    }

    /**
     * This method is called when the negotiation has finished. It can process the
     * final agreement.
     * 
     * @param agreements
     */
    protected abstract void processAgreements(Agreements agreements);

    protected abstract void processNonAgreements(Agreements agreements);

    protected abstract ActionWithBid myTurn(Object param);

    public ProfileInterface getProfileint() {
        return profileint;
    }

    public void setProfileint(ProfileInterface profileint) {
        this.profileint = profileint;
    }

    public Bid getLastReceivedBid() {
        return lastReceivedBid;
    }

    public void setLastReceivedBid(Bid lastReceivedBid) {
        this.lastReceivedBid = lastReceivedBid;
    }

    public List<File> getDataPaths() {
        return dataPaths;
    }

    public void setDataPaths(List<File> dataPaths) {
        this.dataPaths = dataPaths;
    }

    // public T getParam() {
    // return param;
    // }

    // public void setParam(T param) {
    // this.param = param;
    // }
}
