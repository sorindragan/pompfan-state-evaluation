package geniusweb.pompfan.state;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.nd4j.linalg.api.ndarray.INDArray;

import geniusweb.actions.Action;
import geniusweb.pompfan.distances.ExactSame;
import geniusweb.pompfan.distances.L2Distance;
import geniusweb.pompfan.evaluators.IEvalFunction;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.profile.utilityspace.UtilitySpace;

@JsonAutoDetect(fieldVisibility = Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = HistoryState.class)})
public abstract class AbstractState<T>
        implements Comparable<AbstractState<T>>, L2Distance, ExactSame<T> {
    @JsonBackReference
    private UtilitySpace utilitySpace;
    private AbstractPolicy opponent;
    private T representation;
    private Double negotiationMoment;
    public Class<T> containerClass;

    public AbstractState() {
        super();
    }

    public AbstractState(UtilitySpace utilitySpace, AbstractPolicy opponent) {
        super();
        this.utilitySpace = utilitySpace;
        this.opponent = opponent;
    }

    public Double getTime() {
        return negotiationMoment;
    }

    public AbstractState<T> setTime(Double currentTime) {
        this.negotiationMoment = currentTime;
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
    public abstract AbstractState<T> copyState();

    public abstract T getCurrentState();

    public abstract Double computeStateDistance(T otherState);

    public abstract Double evaluate();

}
