package geniusweb.custom.state;


import org.nd4j.linalg.api.ndarray.INDArray;

import geniusweb.actions.Action;
import geniusweb.custom.distances.CosineSimilarity;
import geniusweb.custom.distances.ExactSame;
import geniusweb.custom.distances.L2Distance;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

public abstract class AbstractState<T>
        implements Comparable<AbstractState<T>>, CosineSimilarity, L2Distance, ExactSame<T> {
    private Domain domain;
    private AbstractPolicy opponent;
    private T representation;
    public Class<T> containerClass;

    public AbstractState(Domain domain, AbstractPolicy opponent) {
        super();
        this.domain = domain;
        this.opponent = opponent;
    }



    public Domain getDomain() {
        return domain;
    }

    public AbstractState<T> setDomain(Domain stateRepresentation) {
        this.domain = stateRepresentation;
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

    public abstract String getStringRepresentation();

    public abstract AbstractState<T> updateState(Action nextAction) throws StateRepresentationException;

    public abstract T getCurrentState();

    public abstract Double computeDistance(T otherState);





}
