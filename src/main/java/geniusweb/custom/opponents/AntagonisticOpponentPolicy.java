package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

/**
 * AntagonisticAgentPolicy
 */

public class AntagonisticOpponentPolicy extends AbstractPolicy {

    private final BigDecimal SYMPATHY = new BigDecimal(0.5f);
    @JsonIgnore
    private BidsWithUtility allBids;
    @JsonIgnore
    private Interval possibleRange;
    @JsonIgnore
    private Interval searchRange;
    @JsonIgnore
    private ImmutableList<Bid> possibleBids;

    public AntagonisticOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, name);
        this.allBids = new BidsWithUtility((LinearAdditive) utilitySpace);
        this.possibleRange = this.allBids.getRange();
        this.searchRange = new Interval(this.possibleRange.getMin(),
                this.possibleRange.getMin().multiply(SYMPATHY.add(new BigDecimal(1))));
        this.possibleBids = this.allBids.getBids(this.searchRange);
    }

    public AntagonisticOpponentPolicy(UtilitySpace uSpace) {
        super(uSpace, "Antagonistic");
        this.allBids = new BidsWithUtility((LinearAdditive) uSpace);
        this.possibleRange = this.allBids.getRange();
        this.searchRange = new Interval(this.possibleRange.getMin(),
                this.possibleRange.getMin().multiply(SYMPATHY.add(new BigDecimal(1))));
        this.possibleBids = this.allBids.getBids(this.searchRange);
    }

    @Override
    public Action chooseAction() {
        long i = this.getRandom().nextInt(this.possibleBids.size().intValue());
        Bid bid = this.possibleBids.get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction();
    }

}