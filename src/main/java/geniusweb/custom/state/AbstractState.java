package geniusweb.custom.state;

import java.util.ArrayList;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

public abstract class AbstractState implements Comparable<AbstractState> {
    private Domain domain;
    private AbstractPolicy opponent;
    private StateRepresentation<?> representation;

    public AbstractState(Domain domain, AbstractPolicy opponent) {
        super();
        this.domain = domain;
        this.opponent = opponent;

    }

    public Domain getDomain() {
        return domain;
    }

    public AbstractState setDomain(Domain stateRepresentation) {
        this.domain = stateRepresentation;
        return this;
    }

    public AbstractPolicy getOpponent() {
        return opponent;
    }

    public AbstractState setOpponent(AbstractPolicy opponent) {
        this.opponent = opponent;
        return this;
    }

    public StateRepresentation<?> getRepresentation() {
        return representation;
    }

    @Override
    public int compareTo(AbstractState o) {
        return this.toString().compareTo(o.toString());
    }

    public abstract String getStringRepresentation();

    public abstract AbstractState setRepresentation(StateRepresentation<?> representation);

    public abstract AbstractState updateState(Action nextAction) throws StateRepresentationException;

    public abstract AbstractState getCurrentState();
}
