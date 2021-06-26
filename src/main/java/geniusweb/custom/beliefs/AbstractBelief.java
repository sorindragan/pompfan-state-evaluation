package geniusweb.custom.beliefs;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import geniusweb.actions.Offer;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;

public abstract class AbstractBelief {
    private HashMap<AbstractPolicy, Double> opponentProbabilities = new HashMap<AbstractPolicy, Double>();
    private AbstractBidDistance distance;


    public AbstractBelief(HashMap<AbstractPolicy, Double> opponentProbabilities, AbstractBidDistance distance) {
        this.opponentProbabilities = opponentProbabilities;
        this.distance = distance;
        
    }
    
    public AbstractBelief(List<AbstractPolicy> listOfOpponents, AbstractBidDistance distance) {
        Double uniformProb = 1d / listOfOpponents.size();
        for (AbstractPolicy opponent : listOfOpponents) {
            this.opponentProbabilities.put(opponent, uniformProb);
        }
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

    public HashMap<AbstractPolicy, Double> getOpponentProbabilities() {
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


    public abstract AbstractBelief updateBeliefs(Offer realObservation, Offer lastAction, AbstractState<?> state);

}
