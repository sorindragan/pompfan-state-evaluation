package geniusweb.custom.agent; // TODO: change name

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import geniusweb.actions.PartyId;
import geniusweb.custom.beliefs.AbstractBelief;
import geniusweb.custom.components.BeliefNode;
import geniusweb.custom.components.Tree;
import geniusweb.custom.helper.Configurator;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.wideners.AbstractWidener;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
  private AbstractBelief currentBelief;
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

    this.setCurrentBelief(negotiationData.getBelief());
    this.setConfiguration(negotiationData.getConfiguration());
    this.setRootNode(negotiationData.getRootNode());
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

  public AbstractBelief getCurrentBelief() {
    return currentBelief;
  }

  public void setCurrentBelief(AbstractBelief currentBelief) {
    this.currentBelief = currentBelief;
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

  public Tree reconstructTree(PartyId me, UtilitySpace utilitySpace, Progress progress) {
    AbstractBelief belief = this.getCurrentBelief();
    AbstractState<?> startState = this.getRootNode().getState();
    Configurator configurator = this.getConfiguration() != null ? this.mapper.convertValue(this.getConfiguration(), Configurator.class) : new Configurator();
    configurator =  configurator.setUtilitySpace(utilitySpace).setListOfOpponents(belief.getOpponents()).setMe(me).build();
    AbstractWidener widener = configurator.getWidener();
    Tree mcts = new Tree(utilitySpace, belief, startState, widener, progress);
    return mcts;
  }

  public void save(File destination) throws JsonGenerationException, JsonMappingException, IOException {
    mapper.writerWithDefaultPrettyPrinter().writeValue(destination, this);
  }
}
