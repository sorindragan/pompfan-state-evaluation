package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;


public class SelfishOpponentPolicy extends AbstractPolicy {
    @JsonIgnore
    private BidsWithUtility allBids;
    @JsonIgnore
    private Interval possibleRange;
    @JsonIgnore
    private Interval searchRange;
    @JsonIgnore
    private ImmutableList<Bid> possibleBids;
    public BigDecimal STUBBORNESS = new BigDecimal(new Random().doubles(1l, 0.5, 1.0).mapToObj(dbl -> String.valueOf(dbl)).findFirst().get());

    @JsonCreator
    public SelfishOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("STUBBORNESS") BigDecimal STUBBORNESS) {
        super(utilitySpace, name);
        this.allBids = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.possibleRange = this.getAllBids().getRange();
        this.searchRange = new Interval(this.getPossibleRange().getMax().multiply(STUBBORNESS),
                this.getPossibleRange().getMax());
        this.possibleBids = this.getAllBids().getBids(this.getSearchRange());
    }

    public SelfishOpponentPolicy(Domain domain) {
        super(domain, "Selfish");
        this.allBids = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.possibleRange = this.getAllBids().getRange();
        this.searchRange = new Interval(this.getPossibleRange().getMax().multiply(STUBBORNESS),
                this.getPossibleRange().getMax());
        this.possibleBids = this.getAllBids().getBids(this.getSearchRange());
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, lastOwnBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid,
            AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        long i = this.getRandom().nextInt(this.possibleBids.size().intValue());
        Bid bid = this.possibleBids.get(BigInteger.valueOf(i));
        Offer offer = new Offer(this.getPartyId(), bid);
        Action action;
        if (isGood(lastReceivedBid)) {
            action = new Accept(this.getPartyId(), lastReceivedBid);
            // System.out.println(this.getName() + " generated ACCEPT");
        } else {
            action = offer;
        }
        // Action action = isGood(lastReceivedBid) ? new Accept(this.getPartyId(), lastReceivedBid) : offer;
        return action;
    }

    

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
