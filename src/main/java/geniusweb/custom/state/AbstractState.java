package geniusweb.custom.state;

import org.nd4j.linalg.api.ndarray.INDArray;

import geniusweb.actions.Action;
import geniusweb.custom.distances.CosineSimilarity;
import geniusweb.custom.distances.ExactSame;
import geniusweb.custom.distances.L2Distance;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public abstract class AbstractState<T>
        implements Comparable<AbstractState<T>>, CosineSimilarity, L2Distance, ExactSame<T> {
    private UtilitySpace utilitySpace;
    private AbstractPolicy opponent;
    private T representation;
    private Double round;
    public Class<T> containerClass;

    public AbstractState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super();
        this.utilitySpace = utilitySpace;
        this.opponent = opponent;
    }

    public Double getRound() {
        return round;
    }

    public AbstractState<T> setRound(Double currentTime) {
        this.round = currentTime;
        return this;
    }

    public UtilitySpace getUtilitySpace() {
        return this.utilitySpace;
    }

    public AbstractState<T> setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
        return this;
    }

    public AbstractPolicy getOpponent() {
        return opponent;
    }

    public AbstractState<T> setOpponent(AbstractPolicy opponent) {
        this.opponent = opponent;
        return this;
    }

    public T getRepresentation() {
        return representation;
    }

    public AbstractState<T> init(T representation) {
        this.representation = representation;
        return this;
    };

    @Override
    public int compareTo(AbstractState<T> o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public Double computeCosineSimiliarity(INDArray arr1, INDArray arr2) {
        return null;
    }

    @Override
    public Double computeL2(INDArray arr1, INDArray arr2) {
        return arr1.distance2(arr2);
    }

    @Override
    public Double computeExactSame(T arr1, T arr2) {
        Boolean compResult = arr1.equals(arr2);
        return compResult ? 1.0 : 0.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractState<?>) {
            AbstractState<?> otherState = (AbstractState<?>) obj;
            return this.getRepresentation().equals(otherState.getRepresentation());
        }
        return false;
    }

    public abstract String getStringRepresentation();

    public abstract AbstractState<T> updateState(Action nextAction, Double time) throws StateRepresentationException;

    public abstract T getCurrentState();

    public abstract Double computeDistance(T otherState);

    public abstract Double evaluate();

}
