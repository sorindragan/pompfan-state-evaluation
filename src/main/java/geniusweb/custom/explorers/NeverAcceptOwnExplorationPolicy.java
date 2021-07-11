package geniusweb.custom.explorers;

import java.math.BigInteger;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class NeverAcceptOwnExplorationPolicy extends AbstractOwnExplorationPolicy {

    public NeverAcceptOwnExplorationPolicy(Domain domain, UtilitySpace utilitySpace, PartyId id) {
        super(domain, "NeverAcceptExplorationPolicy", utilitySpace, id);
    }

    @Override
    public Action chooseAction() {
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid); 
    }

    
}
