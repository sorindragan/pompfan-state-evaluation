package geniusweb.pompfan.beliefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import geniusweb.actions.Offer;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.opponents.AbstractPolicy;
import geniusweb.pompfan.opponents.AbstractPolicyDeserializer;
import geniusweb.pompfan.opponents.AbstractPolicySerializer;
import geniusweb.pompfan.state.AbstractState;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = ParticleFilterBelief.class, name = "ParticleFilterBelief"),
        @Type(value = ParticleFilterWithAcceptBelief.class, name = "ParticleFilterWithAcceptBelief"),
        @Type(value = UniformBelief.class, name = "UniformBelief") })
public abstract class AbstractBelief {

    private List<AbstractPolicy> opponents = null;
    private List<Double> probabilities = null;
    private Double learnedOpponentBias = 0.05d;

    // @JsonProperty("opponentProbabilities")
    // @JsonDeserialize(keyUsing = AbstractPolicyDeserializer.class)
    // @JsonSerialize(keyUsing = AbstractPolicySerializer.class)
    @JsonIgnore
    private Map<AbstractPolicy, Double> opponentProbabilities = new HashMap<AbstractPolicy, Double>();
    private AbstractBidDistance distance;

    @JsonCreator
    public AbstractBelief(List<AbstractPolicy> opponents, List<Double> probabilities, AbstractBidDistance distance) {
        for (int i = 0; i < opponents.size(); i++) {
            this.opponentProbabilities.put(opponents.get(i), probabilities.get(i));
        }
        this.opponents = this.getOpponentProbabilities().keySet().stream().collect(Collectors.toList());
        this.probabilities = this.getOpponentProbabilities().values().stream().collect(Collectors.toList());
        this.distance = distance;
    }

    public AbstractBelief(Map<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        this.opponentProbabilities = opponentProbabilities;
        Double sumVals = this.opponentProbabilities.values().stream().mapToDouble(val -> val).sum();
        this.opponentProbabilities.entrySet().parallelStream()
                .forEach(entry -> this.opponentProbabilities.put(entry.getKey(), entry.getValue() / sumVals));
        this.opponents = this.getOpponentProbabilities().keySet().stream().collect(Collectors.toList());
        this.probabilities = this.getOpponentProbabilities().values().stream().collect(Collectors.toList());
        this.distance = distance;
    }

    public AbstractBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        Double uniformProb = 1d / listOfOpponents.size();
        for (AbstractPolicy opponent : listOfOpponents) {
            this.opponentProbabilities.put(opponent, uniformProb);
        }
        this.opponents = this.getOpponentProbabilities().keySet().stream().collect(Collectors.toList());
        this.probabilities = this.getOpponentProbabilities().values().stream().collect(Collectors.toList());
        this.distance = distance;
    }

    public AbstractPolicy sampleOpponent() {
        Double index = new Random().nextDouble();
        Double sum = 0d;
        for (Entry<AbstractPolicy, Double> opponent : this.opponentProbabilities.entrySet()) {
            sum += opponent.getValue();
            if (sum > index) {
                return opponent.getKey(); // This might crash, but shouldn't
            }
        }
        return this.opponentProbabilities.keySet().iterator().next();
    }

    // @JsonProperty("opponentProbabilities")
    // @JsonDeserialize(keyUsing = AbstractPolicyDeserializer.class)
    public Map<AbstractPolicy, Double> getOpponentProbabilities() {
        return opponentProbabilities;
    }

    public AbstractBelief setOpponentProbabilities(HashMap<AbstractPolicy, Double> opponentProbabilities) {
        this.opponentProbabilities = opponentProbabilities;
        return this;
    }

    public AbstractBidDistance getDistance() {
        return distance;
    }

    public void setDistance(AbstractBidDistance distance) {
        this.distance = distance;
    }

    // public abstract AbstractBelief updateBeliefs(Offer realObservation, Offer
    // lastAction, AbstractState<?> state);
    public void setOpponents(List<AbstractPolicy> opponents) {
        this.opponents = opponents;
    }

    public List<Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    public abstract AbstractBelief updateBeliefs(Offer newOppObservation, Offer lastRealAgentAction,
            Offer lastRealOppAction, AbstractState<?> state);

    @Override
    public String toString() {

        Map<String, Double> opponents = this.getOpponentProbabilities().entrySet().stream().collect(
                Collectors.groupingBy(opp -> opp.getKey().getName(), Collectors.summingDouble(opp -> opp.getValue())));
        return opponents.toString();
    }

    // @JsonIgnore
    public List<AbstractPolicy> getOpponents() {
        return this.opponents;
    }

    public AbstractBelief addNewOpponents(List<AbstractPolicy> newOpponents) {
        Double slicePerOpponent = 1.0 - this.getLearnedOpponentBias(); 
        Double sliecePerNewOpponent = this.getLearnedOpponentBias()/newOpponents.size();
        List<Double> oldProbsAdjusted = this.getProbabilities().stream().map(e -> e*slicePerOpponent).collect(Collectors.toList());
        List<Double> newProbs = IntStream.range(0, newOpponents.size()).mapToDouble(e -> sliecePerNewOpponent).boxed().collect(Collectors.toList());
        this.setProbabilities(oldProbsAdjusted);;
        this.getOpponents().addAll(newOpponents);
        this.getProbabilities().addAll(newProbs);
        
        for (int i = 0; i < this.getOpponents().size(); i++) {
            this.getOpponentProbabilities().put(this.getOpponents().get(i), this.getProbabilities().get(i));
        }
        return this;
    }

    public Double getLearnedOpponentBias() {
        return learnedOpponentBias;
    }

    public void setLearnedOpponentBias(Double learnedOpponentBias) {
        this.learnedOpponentBias = learnedOpponentBias;
    }
}
