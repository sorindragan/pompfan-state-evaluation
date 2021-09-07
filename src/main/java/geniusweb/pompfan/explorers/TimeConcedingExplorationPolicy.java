package geniusweb.pompfan.explorers;

import java.math.BigDecimal;
import java.util.Random;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.issuevalue.Bid;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class TimeConcedingExplorationPolicy extends AbstractOwnExplorationPolicy {

    private BidsWithUtility bidutils;

    public TimeConcedingExplorationPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
        this.bidutils = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        Action action;
        Bid bid;
        // progressively open the bounds of explored bids
        double lowerBound = 1.0 - (state.getTime() / 2);

        ImmutableList<Bid> options = this.getBidutils().getBids(new Interval(BigDecimal.valueOf(lowerBound),
                this.getUtilitySpace().getUtility(this.getBidutils().getExtremeBid(true))));

        if (options.size().intValue() < 1) {
            return new Offer(this.getPartyId(), this.getAllBids().getExtremeBid(true));
        }
        bid = options.get(new Random().nextInt(options.size().intValue()));
        action = new Offer(this.getPartyId(), bid);
        if (lastReceivedBid == null) {
            return action;
        }

        BigDecimal utilToBeOffered = this.getUtilitySpace().getUtility(bid);
        BigDecimal utilFromOpp = this.getUtilitySpace().getUtility(lastReceivedBid);
        if (utilToBeOffered.doubleValue() <= utilFromOpp.doubleValue()) {
            action = new Accept(this.getPartyId(), lastReceivedBid);
        }

        return action;
    }

    @Override
    protected void init() {
        assert true;
    }

    public BidsWithUtility getBidutils() {
        return bidutils;
    }

    public void setBidutils(BidsWithUtility bidutils) {
        this.bidutils = bidutils;
    }

}
