package geniusweb.custom.opponents;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;

public interface CommonOpponentInterface {
    public abstract Action chooseAction();

    public abstract Action chooseAction(AbstractState<?> state);

    public abstract Action chooseAction(Bid lastReceivedBid, AbstractState<?> state);
    
    public abstract Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state);

    // public abstract Action chooseAction(Bid lastAgentBid, Bid lastReceivedBid, AbstractState<?> state, Long depthRound);
}
