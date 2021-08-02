package geniusweb.pompfan.opponents;

import geniusweb.actions.Action;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.AbstractState;

public interface CommonOpponentInterface {
    public abstract Action chooseAction();

    public abstract Action chooseAction(AbstractState<?> state);

    public abstract Action chooseAction(Bid lastReceivedBid, AbstractState<?> state);
    
    public abstract Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state);

}
