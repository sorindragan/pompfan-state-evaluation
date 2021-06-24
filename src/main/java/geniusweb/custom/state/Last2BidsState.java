package geniusweb.custom.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.custom.components.Opponent;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Domain;

public class Last2BidsState extends HistoryState {

    public Last2BidsState(Domain domain, AbstractPolicy opponent) {
        super(domain, opponent);
    }

    private ArrayList<Action> history;

    @Override
    public Double evaluate() {
        return super.evaluate();
    }

}
