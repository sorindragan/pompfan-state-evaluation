package geniusweb.pompfan.explorers;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class SelfishReluctantOwnExplorerPolicy extends AbstractOwnExplorationPolicy {
    private static final BigDecimal STUBBORNESS = new BigDecimal("0.4");

    public SelfishReluctantOwnExplorerPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }

    
    @Override
    public Action chooseAction(Bid lastOpponentBid, Bid lastAgentBid, AbstractState<?> state) {
        Action action;
        Bid bid;

        if (lastOpponentBid == null) {
            return new Offer(this.getPartyId(), this.getAllBids().getExtremeBid(true));
        }
        
        long i = this.getRandom().nextInt(this.getPossibleBids().size().intValue());
        Bid ownBidCandidate = this.getPossibleBids().get(i);
        bid = shouldAccept(ownBidCandidate, lastOpponentBid) ? ownBidCandidate : lastOpponentBid;
    
        if (bid == lastOpponentBid && this.getUtilitySpace().getUtility(lastOpponentBid).doubleValue() < STUBBORNESS.doubleValue()) {
            return new Offer(this.getPartyId(), this.getAllBids().getExtremeBid(true));
        }

        // 2 pcent of the time we do accepts
        if (this.getRandom().nextInt(100) > 98) {
            return new Accept(this.getPartyId(), lastOpponentBid);
        }

        action = new Offer(this.getPartyId(), bid);
        return action;
    }

    private boolean shouldAccept(Bid bid, Bid oppBid) {
        if (bid == null)
            return false;
        BigDecimal sample = this.getUtilitySpace().getUtility(bid);
        BigDecimal target = this.getUtilitySpace().getUtility(oppBid);
        return (sample.doubleValue() < target.doubleValue());
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);
    }

}
