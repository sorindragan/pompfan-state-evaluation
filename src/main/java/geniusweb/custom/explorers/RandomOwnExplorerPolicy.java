package geniusweb.custom.explorers;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class RandomOwnExplorerPolicy extends AbstractOwnExplorationPolicy {

    private static final BigDecimal STUBBORNESS = new BigDecimal("0.4");

    public RandomOwnExplorerPolicy(UtilitySpace utilitySpace,  PartyId id) {
        super(utilitySpace, id);
    }

    @Override
    public Action chooseAction(Bid lastOpponentBid, Bid lastAgentBid, AbstractState<?> state) {
        Action action;
        Bid bid;
     
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        bid = this.getBidspace().get(i);

        if (lastOpponentBid == null) {
            return new Offer(this.getPartyId(), bid);
        }
        action = shouldAccept(bid, lastOpponentBid) ? new Accept(this.getPartyId(), lastOpponentBid) : new Offer(this.getPartyId(), bid);
        return action;
    }

    private boolean shouldAccept(Bid bid, Bid oppBid) {
        if (bid == null)
            return false;
        BigDecimal sample = this.getUtilitySpace().getUtility(bid);
        BigDecimal target = this.getUtilitySpace().getUtility(oppBid);
        return (sample.doubleValue() <= target.doubleValue()) && (target.doubleValue() >= STUBBORNESS.doubleValue());
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);        
    }

  
}
