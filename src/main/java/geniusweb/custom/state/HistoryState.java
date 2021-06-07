package geniusweb.custom.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

public class HistoryState extends AbstractState<ArrayList<Action>> {

    private ArrayList<Action> history;

    public HistoryState(Domain domain, AbstractPolicy opponent) {
        super(domain, opponent);
    }

    public ArrayList<Action> getHistory() {
        return this.getRepresentation();
    }

    public void setHistory(ArrayList<Action> history) {
        this.init(history);
    }

    @Override
    public String getStringRepresentation() {
        return this.getHistory().stream().map(Object::toString).collect(Collectors.joining("->"));
    }

    @Override
    public AbstractState<ArrayList<Action>> updateState(Action nextAction) throws StateRepresentationException {
        ArrayList<Action> representation = new ArrayList<Action>(this.getRepresentation());
        representation.add(nextAction);
        return new HistoryState(this.getDomain(), this.getOpponent()).init(representation);
    }

    @Override
    public ArrayList<Action> getCurrentState() {
        // TODO Auto-generated method stub
        return null;
    }

}
