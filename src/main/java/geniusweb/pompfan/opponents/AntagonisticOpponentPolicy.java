package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

/**
 * AntagonisticAgentPolicy: aims for a purely competitive behaviour
 */

public class AntagonisticOpponentPolicy extends AbstractPolicy {

    /**
     *
     */
    public static final String ANTAGONISTIC = "Antagonistic";
    private BigDecimal SYMPATHY = new BigDecimal(new Random().doubles(1l, 0.0, 0.5).mapToObj(dbl -> String.valueOf(dbl)).findFirst().get());
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
        this.searchRange = new Interval(BigDecimal.ZERO,
                this.possibleRange.getMin().multiply(SYMPATHY.add(new BigDecimal(1))));
        this.possibleBids = this.allBids.getBids(this.searchRange);
    }

    public AntagonisticOpponentPolicy(UtilitySpace uSpace) {
        super(uSpace, ANTAGONISTIC);
        this.allBids = new BidsWithUtility((LinearAdditive) uSpace);
        this.possibleRange = this.allBids.getRange();
        this.searchRange = new Interval(BigDecimal.ZERO,
                this.possibleRange.getMin().multiply(SYMPATHY.add(new BigDecimal(1))));
        this.possibleBids = this.allBids.getBids(this.searchRange);
        // this.setPartyId(new PartyId(ANTAGONISTIC+"_"+SYMPATHY.round(new MathContext(4)).toString().replace(".", "")));
    }


	public AntagonisticOpponentPolicy(UtilitySpace uSpace, String name) {
        super(uSpace, name);
        this.allBids = new BidsWithUtility((LinearAdditive) uSpace);
        this.possibleRange = this.allBids.getRange();
        this.searchRange = new Interval(BigDecimal.ZERO,
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

    public BigDecimal getSYMPATHY() {
        return SYMPATHY;
    }

    public void setSYMPATHY(BigDecimal sYMPATHY) {
        SYMPATHY = sYMPATHY;
    }

    

}