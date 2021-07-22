package geniusweb.custom.explorers;

import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

// Class that never explores bids below what the opponent is already giving us!!!
public class HighSelfEsteemOwnExplorationPolicy extends AbstractOwnExplorationPolicy {

    public HighSelfEsteemOwnExplorationPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
        //TODO Auto-generated constructor stub
    }


    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        // TODO Never explore below opponent last bid utility!!!
        return super.chooseAction(lastReceivedBid, lastOwnBid, state);
    }

    @Override
    protected void init() {
        // TODO Auto-generated method stub
        
    }
    
}
