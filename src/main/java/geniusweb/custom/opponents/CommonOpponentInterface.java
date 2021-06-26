package geniusweb.custom.opponents;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public interface CommonOpponentInterface {
    public abstract Action chooseAction();
    public abstract Action chooseAction(Bid lastAgentBid);
    public abstract Action chooseAction(Bid lastAgentBid, AbstractState<?> state);
}
