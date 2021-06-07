package geniusweb.custom.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

public class HistoryState extends AbstractState {

    private Collection<Action> history;

    public HistoryState(Domain domain, AbstractPolicy opponent) {
        super(domain, opponent);
    }

    public HistoryState(Domain domain, AbstractPolicy opponent, HistoryState hist) {
        super(domain, opponent);
        this.history = new ArrayList<Action>(hist.getHistory());
    }

    public HistoryState(Domain domain, AbstractPolicy opponent, HistoryState hist, Action nextAction) {
        super(domain, opponent);
        this.history = new ArrayList<Action>(hist.getHistory());
        this.history.add(nextAction);
    }

    public Collection<Action> getHistory() {
        return history;
    }

    public void setHistory(Collection<Action> history) {
        this.history = history;
    }

    @Override
    public String getStringRepresentation() {
        return this.getHistory().stream().map(Object::toString).collect(Collectors.joining("->"));
    }

    @Override
    public AbstractState updateState(Action nextAction) throws StateRepresentationException {
        Representation representation = new Representation(this.getHistory());
        representation.add(nextAction);
        return this;
    }

    @Override
    public AbstractState getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractState setRepresentation(StateRepresentation<?> representation) {
        Representation customRepr = (Representation) representation;
        this.history = customRepr.getOriginalObject();
        return this;
    }


    public class Representation extends ArrayList<Action> implements StateRepresentation<ArrayList<Action>> {

        public Representation(Collection<Action> freq) {
            super(freq);
        }

        @Override
        public ArrayList<Action> getOriginalObject() {
            return this;
        }
    }

}
