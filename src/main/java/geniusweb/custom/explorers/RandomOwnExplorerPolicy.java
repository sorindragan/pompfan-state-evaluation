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

    private static final BigDecimal STUBBORNESS = new BigDecimal(0.5);

    public RandomOwnExplorerPolicy(UtilitySpace utilitySpace,  PartyId id) {
        super(utilitySpace, id);
    }

    @Override
    public Action chooseAction(Bid lastOpponentBid, Bid lastAgentBid, AbstractState<?> state) {
        Action action;
        Bid bid;
        // if (lastOpponentBid == null) {
        //     bid = this.getAllBids().getExtremeBid(true);
        // } else {
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        bid = this.getBidspace().get(i);
        // }

        if (lastOpponentBid == null) {
            return new Offer(this.getPartyId(), bid);
        }
        action = isGood(bid) ? new Offer(this.getPartyId(), bid) : new Accept(this.getPartyId(), lastOpponentBid);
        return action;
    }

    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;
        BigDecimal sample = this.getUtilitySpace().getUtility(bid);
        return sample.compareTo(this.getSTUBBORNESS()) >= 0 ? true : false;
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);        
    }

  
}
