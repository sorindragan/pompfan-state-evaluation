package geniusweb.custom.beliefs;

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import geniusweb.custom.strategies.AbstractPolicy;

public abstract class AbstractBelief {
    private HashMap<AbstractPolicy, Double> opponentProbabilities = new HashMap<AbstractPolicy, Double>();


    public AbstractBelief(HashMap<AbstractPolicy, Double> opponentProbabilities) {
        this.opponentProbabilities = opponentProbabilities;
    }

    public AbstractBelief(List<AbstractPolicy> listOfOpponents) {
        Double uniformProb = 1d / listOfOpponents.size();
        for (AbstractPolicy opponent : listOfOpponents) {
            this.opponentProbabilities.put(opponent, uniformProb);
        }
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

    public abstract AbstractBelief updateBeliefs();

}
