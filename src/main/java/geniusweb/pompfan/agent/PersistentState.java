package geniusweb.pompfan.agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.pompfan.beliefs.AbstractBelief;
import geniusweb.pompfan.components.BeliefNode;
import geniusweb.pompfan.components.Tree;
import geniusweb.pompfan.helper.Configurator;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.opponents.ImitateOpponentPolicy;
import geniusweb.pompfan.opponents.OpponentParticleCreator;
import geniusweb.pompfan.opponents.SimpleOpponentModelPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.wideners.AbstractWidener;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;

/**
 * This class can hold the persistent state of your agent. You can off course
 * also write something else to the file path that is provided to your agent,
 * but this provides an easy usable method. This object is serialized using
 * Jackson. NOTE that Jackson can serialize many default java classes, but not
 * custom classes out-of-the-box.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class PersistentState {

  private Double averageUtility = 0.0;
  private Integer negotiations = 0;
  private Map<String, Double> avgMaxUtilityOpponent = new HashMap<String, Double>();
  private Map<String, Integer> opponentEncounters = new HashMap<String, Integer>();
  private Map<String, AbstractBelief> allOpponentBeliefs = new HashMap<String, AbstractBelief>();
  private List<List<Action>> allOpponentActions = new ArrayList<List<Action>>();
  private List<String> encounteredOpponentArchive = new ArrayList<String>();

  private HashMap<String, Object> configuration;
  private BeliefNode rootNode;

  @JsonIgnore
  private ObjectMapper mapper = new ObjectMapper();

  /**
   * Update the persistent state with a negotiation data of a previous negotiation
   * session
   *
   * @param negotiationData NegotiationData class holding the negotiation data
   *                        that is obtain during a negotiation session.
   */
  public void update(NegotiationData negotiationData) {
    // Keep track of the average utility that we obtained
    this.averageUtility = (this.averageUtility * negotiations + negotiationData.getAgreementUtil())
        / (negotiations + 1);

    // Keep track of the number of negotiations that we performed
    negotiations++;

    // Get the name of the opponent that we negotiated against
    String opponent = negotiationData.getOpponentName();

    // Check for safety
    if (opponent != null) {
      // Update the number of encounters with an opponent
      Integer encounters = opponentEncounters.containsKey(opponent) ? opponentEncounters.get(opponent) : 0;
      opponentEncounters.put(opponent, encounters + 1);
      // Track the average value of the maximum that an opponent has offered us across
      // multiple negotiation sessions
      Double avgUtil = avgMaxUtilityOpponent.containsKey(opponent) ? avgMaxUtilityOpponent.get(opponent) : 0.0;
      avgMaxUtilityOpponent.put(opponent,
          (avgUtil * encounters + negotiationData.getMaxReceivedUtil()) / (encounters + 1));
    }

    this.addBelief(opponent, negotiationData.getBelief());
    this.setConfiguration(negotiationData.getConfiguration());
    this.setRootNode(negotiationData.getRootNode());
    this.getAllOpponentActions().add(negotiationData.getRealOppHistory());
    this.getEncounteredOpponentArchive().add(opponent);

  }

  public List<String> getEncounteredOpponentArchive() {
    return encounteredOpponentArchive;
  }

  public void setEncounteredOpponentArchive(List<String> encounteredOpponentArchive) {
    this.encounteredOpponentArchive = encounteredOpponentArchive;
  }

  public List<List<Action>> getAllOpponentActions() {
    return allOpponentActions;
  }

  public void setAllOpponentActions(List<List<Action>> allOpponentActions) {
    this.allOpponentActions = allOpponentActions;
  }

  public BeliefNode getRootNode() {
    return rootNode;
  }

  public void setRootNode(BeliefNode rootNode) {
    this.rootNode = rootNode;
  }

  public HashMap<String, Object> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(HashMap<String, Object> configuration) {
    this.configuration = configuration;
  }

  public void addBelief(String opponent, AbstractBelief abstractBelief) {
    this.getAllOpponentBeliefs().put(opponent, abstractBelief);
  }

  public Double getAvgMaxUtility(String opponent) {
    if (avgMaxUtilityOpponent.containsKey(opponent)) {
      return avgMaxUtilityOpponent.get(opponent);
    }
    return null;
  }

  public Integer getOpponentEncounters(String opponent) {
    if (opponentEncounters.containsKey(opponent)) {
      return opponentEncounters.get(opponent);
    }
    return null;
  }

  public Boolean knownOpponent(String opponent) {
    return opponentEncounters.containsKey(opponent);
  }

  public Tree reconstructTree(PartyId me, UtilitySpace utilitySpace, Progress progress, String opponent,
      Long numParticlesPerOpponent) {
    Map<String, AbstractBelief> allBeliefs = this.getAllOpponentBeliefs();
    AbstractBelief belief = allBeliefs.get(opponent);
    List<AbstractPolicy> listOfOpponents;
    if (belief == null) {
      listOfOpponents = OpponentParticleCreator.generateOpponentParticles(utilitySpace, numParticlesPerOpponent);
    } else {
      listOfOpponents = belief.getOpponents();
    }

    List<AbstractPolicy> newOpponents = new ArrayList<>();
    for (int i = 0; i < numParticlesPerOpponent; i++) {
      int cnt = 0;
      for (List<Action> oppActions : this.getAllOpponentActions()) {
        if (oppActions.size() < 2) {
            continue;
        }
        ArrayList<Action> l1 = new ArrayList<Action>(oppActions);
        ArrayList<Action> l2 = new ArrayList<Action>(oppActions);
        String nameOfOpponent = this.getEncounteredOpponentArchive().get(cnt);
        String[] nameSplit = nameOfOpponent.split("_");
        AbstractPolicy imitator = new ImitateOpponentPolicy(utilitySpace.getDomain(), "Imitator" + nameSplit[nameSplit.length-1],
            l1);
        AbstractPolicy freqModel = new SimpleOpponentModelPolicy(utilitySpace.getDomain(),
            "OpponentModel" + nameSplit[nameSplit.length-1], l2);
        newOpponents.add(imitator);
        newOpponents.add(freqModel);
        cnt++;
      }
    }

    
    
    Configurator configurator = this.getConfiguration() != null
        ? this.mapper.convertValue(this.getConfiguration(), Configurator.class)
        : new Configurator();

    configurator = configurator.setUtilitySpace(utilitySpace).setListOfOpponents(listOfOpponents).setMe(me).build();
    AbstractBelief belief2use = belief == null ? configurator.getBelief() : belief;
    belief2use = belief2use.addNewOpponents(newOpponents);
    this.setEncounteredOpponentArchive(new ArrayList<String>());
    this.setAllOpponentActions(new ArrayList<List<Action>>());


    AbstractWidener widener = configurator.getWidener();
    AbstractState<?> startState = configurator.getInitState();
    Tree mcts = new Tree(utilitySpace, belief2use, startState, widener, progress);
    return mcts;
  }

  public void save(File destination) throws JsonGenerationException, JsonMappingException, IOException {
    mapper.writerWithDefaultPrettyPrinter().writeValue(destination, this);
  }

  public Map<String, AbstractBelief> getAllOpponentBeliefs() {
    return allOpponentBeliefs;
  }

  public void setAllOpponentBeliefs(Map<String, AbstractBelief> allOpponentBeliefs) {
    this.allOpponentBeliefs = allOpponentBeliefs;
  }

  public PersistentState learn(){
    // Future TODO: Move all the learning here
    return this;
  }
}
