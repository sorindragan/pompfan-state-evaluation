package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class SelfishOpponentPolicy extends AbstractPolicy {

    private BidsWithUtility allBids;
    private Interval possibleRange;
    private Interval searchRange;
    private ImmutableList<Bid> possibleBids;
    private final BigDecimal STUBBORNESS = new BigDecimal(0.8f);

    public SelfishOpponentPolicy(Domain domain) {
        super(domain, "SelfishAgent");
        this.allBids = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.possibleRange = this.getAllBids().getRange();
        this.searchRange = new Interval(this.getPossibleRange().getMax().multiply(STUBBORNESS),
                this.getPossibleRange().getMax());
        this.possibleBids = this.getAllBids().getBids(this.getSearchRange());
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        Action action;
        Offer offer = (Offer) this.chooseAction();
        action = isGood(lastReceivedBid) ? new Accept(this.getPartyId(), lastReceivedBid) : offer;
        return action;
    }

    // @Override
    // public Action chooseAction() {
    //     long i = this.getRandom().nextInt(this.getPossibleBids().size().intValue());
    //     Bid bid = this.getPossibleBids().get(BigInteger.valueOf(i));
    //     return new Offer(this.getPartyId(), bid);
    // }

    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;
        BigDecimal sample = this.getUtilitySpace().getUtility(bid);
        return sample.compareTo(STUBBORNESS) >= 0 ? true : false;
    }

    public BidsWithUtility getAllBids() {
        return allBids;
    }

    public void setAllBids(BidsWithUtility allBids) {
        this.allBids = allBids;
    }

    public Interval getPossibleRange() {
        return possibleRange;
    }

    public void setPossibleRange(Interval possibleRange) {
        this.possibleRange = possibleRange;
    }

    public Interval getSearchRange() {
        return searchRange;
    }

    public void setSearchRange(Interval searchRange) {
        this.searchRange = searchRange;
    }

    public ImmutableList<Bid> getPossibleBids() {
        return possibleBids;
    }

    public void setPossibleBids(ImmutableList<Bid> possibleBids) {
        this.possibleBids = possibleBids;
    }

}
