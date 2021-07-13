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

public class SelfishReluctantOwnExplorerPolicy extends AbstractOwnExplorationPolicy {
    private static final BigDecimal STUBBORNESS = new BigDecimal(0.5);

    public SelfishReluctantOwnExplorerPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }

    @Override
    public Action chooseAction(Bid lastOpponentBid, Bid lastAgentBid, AbstractState<?> state) {
        Action action;
        Bid bid;
        if (lastOpponentBid == null) {
            bid = this.getAllBids().getExtremeBid(true);
        } else {
            long i = this.getRandom().nextInt(this.getPossibleBids().size().intValue());
            Bid ownBidCandidate = this.getPossibleBids().get(i);
            Boolean isBidHigherThanOpponent = this.getUtilitySpace().getUtility(ownBidCandidate)
                    .compareTo(this.getUtilitySpace().getUtility(lastOpponentBid)) >= 0;
            bid = isBidHigherThanOpponent ? ownBidCandidate : lastOpponentBid;
        }
        action = new Offer(this.getPartyId(), bid);
        return action;
    }

    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;
        BigDecimal sample = this.getUtilitySpace().getUtility(bid);
        return sample.compareTo(STUBBORNESS) >= 0 ? true : false;
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);
    }

}
