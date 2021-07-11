package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;

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
    private BidsWithUtility allBids;
    private Interval possibleRange;
    private Interval searchRange;
    private ImmutableList<Bid> possibleBids;

    public AntagonisticOpponentPolicy(UtilitySpace uSpace) {
        super(uSpace, "Antagonist");
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

    private boolean isGood(Bid bid) {
        BigDecimal yourUtility = this.getUtilitySpace().getUtility(bid);
        return yourUtility.compareTo(SYMPATHY) == -1;
    }

}