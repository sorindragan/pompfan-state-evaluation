package geniusweb.pompfan.explorers;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class SelfishNeverAcceptOwnExplorerPolicy extends AbstractOwnExplorationPolicy {
    private static final BigDecimal STUBBORNESS = new BigDecimal("0.99");

    public SelfishNeverAcceptOwnExplorerPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }

    @Override
    public Action chooseAction(Bid lastOpponentBid, Bid lastAgentBid, AbstractState<?> state) {
        long i = this.getRandom().nextInt(this.getPossibleBids().size().intValue());
        Bid bid = this.getPossibleBids().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid);
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);        
    }

   

}
