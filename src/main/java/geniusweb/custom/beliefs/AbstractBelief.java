package geniusweb.custom.beliefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

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
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.opponents.AbstractPolicyDeserializer;
import geniusweb.custom.opponents.AbstractPolicySerializer;
import geniusweb.custom.state.AbstractState;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = ParticleFilterBelief.class, name = "ParticleFilterBelief"),
        @Type(value = ParticleFilterWithAcceptBelief.class, name = "ParticleFilterWithAcceptBelief"),
        @Type(value = UniformBelief.class, name = "UniformBelief") })
public abstract class AbstractBelief {

    List<AbstractPolicy> opponents = null;
    List<Double> probabilities = null;

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
}
