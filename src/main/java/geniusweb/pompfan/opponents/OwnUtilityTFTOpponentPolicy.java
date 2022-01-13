package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilityTFTOpponentPolicy extends AbstractPolicy {

    @JsonIgnore
    private ExtendedUtilSpace extendedspace;
    private Bid myLastbid = null;
    private Bid maxBid;
    private BidsWithUtility bidutils;
    private final boolean DEBUG = false;

    @JsonCreator
    public OwnUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, 
            @JsonProperty("name") String name) {
        super(uSpace, name);
        this.bidutils = new BidsWithUtility((LinearAdditive) uSpace);
        this.maxBid = this.bidutils.getExtremeBid(true);
    }
    
    public OwnUtilityTFTOpponentPolicy(Domain domain) {
        super(domain, "OwnUtilTFT");
        this.bidutils = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.maxBid = this.bidutils.getExtremeBid(true);
    }
    
    // the ractive agents should have this method implemented
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        
        ActionWithBid action;
        this.extendedspace = new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace());

        myLastbid  = lastOwnBid;
        if (second2lastReceivedBid == null) {
            // not enough information known
            Bid bid = this.maxBid;
            action = new Offer(this.getPartyId(), bid);
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = second2lastReceivedBid;
        Bid justLastOpponentBid = lastReceivedBid;
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal utilityGoal = isConcession
                ? this.getUtilitySpace().getUtility(myLastbid).subtract(difference.abs())
                        .max((this.extendedspace.getMax().add(this.extendedspace.getMin()))
                                .divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(this.extendedspace.getMax());

        Bid selectedBid = computeNextBid(utilityGoal);
        double time = state.getTime();

        if (DEBUG) {
            System.out.println("============================");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
        }

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                && time > 0.8
                        ? new Accept(this.getPartyId(), justLastOpponentBid)
                        : new Offer(this.getPartyId(), selectedBid);
    }


    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
            return this.chooseAction(lastReceivedBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    @Override
    public Action chooseAction(AbstractState<?> state) {
        // this should not happen
        return super.chooseAction(state);
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {
        ImmutableList<Bid> options = this.extendedspace.getBids(utilityGoal);
        if (options.size() == BigInteger.ZERO) {
            // System.out.println("WARNING: PARTICLE TOLERANCE TOO LOW");

            options = this.bidutils.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.1")), utilityGoal));
            // System.out.println(options.size() + " " + utilityGoal.doubleValue());
        }
        try {
            // this should hardly happen
            return options.get(options.size().intValue() - 1);
        } catch (Exception e) {
            System.out.println("PARTICLE: No bid in that interval. " + utilityGoal.doubleValue());
            options = this.bidutils.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.2")), utilityGoal.add(new BigDecimal("0.1"))));
            return options.get(options.size().intValue() - 1);
        }
    }
}
