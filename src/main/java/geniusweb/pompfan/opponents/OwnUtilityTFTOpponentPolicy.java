package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
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
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilityTFTOpponentPolicy extends AbstractPolicy {

    private static double utilGapForConcession = 0.02;
    @JsonIgnore
    BidsWithUtility bidsWithinUtil;

    
    @JsonCreator
    public OwnUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, 
            @JsonProperty("name") String name) {
        super(uSpace, name);
        this.setBidsWithinUtil(new BidsWithUtility((LinearAdditive) this.getUtilitySpace()));
    }
    
    public OwnUtilityTFTOpponentPolicy(Domain domain) {
        super(domain, "OwnUtilTFT");
        this.setBidsWithinUtil(new BidsWithUtility((LinearAdditive) this.getUtilitySpace()));;
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
        boolean isConcession = false;
        AllBidsList allPossibleBids = new AllBidsList(this.getUtilitySpace().getDomain());
        BigDecimal cntBidSpace = new BigDecimal(allPossibleBids.size());
        BigDecimal concessionThreshold = BigDecimal.ONE.divide(cntBidSpace, 5, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(2));
        Bid justLastOpponentBid = this.getBidsWithinUtil().getExtremeBid(true);
        if (state instanceof HistoryState) {
            double difference = 0.0;
            ArrayList<Bid> bidHistory = new ArrayList<>(((HistoryState) state).getHistory().stream()
                    .map(this::extractBidFromAction).collect(Collectors.toList()));
            int historySize = bidHistory.size();
            Bid lastOfferedBid = this.getBidsWithinUtil().getExtremeBid(true);
            if (historySize > 3) {
                Bid lastLastOpponentBid = bidHistory.get(historySize - 3);
                justLastOpponentBid = bidHistory.get(historySize - 1);
                difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                        .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid)).doubleValue();
                isConcession = difference >= concessionThreshold.doubleValue() ? true : false;
                lastOfferedBid = bidHistory.get(historySize - 2);
            } else {
                // friendly start by making concessions
                isConcession = true;
            }

            ImmutableList<Bid> options;
            long i;
            // also concede
            if (isConcession) {
                // I needed something like getBidNearUtility
                options = this.getBidsWithinUtil()
                        .getBids(new Interval(
                                BigDecimal.valueOf(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue()
                                - (1 * difference)).max(new BigDecimal("0.0")),
                                this.getUtilitySpace().getUtility(lastOfferedBid)));

                if (options.size().intValue() == 0) {
                    options = this.getBidsWithinUtil()
                            .getBids(
                                    new Interval(
                                    BigDecimal.valueOf(this.getUtilitySpace().getUtility(lastOfferedBid)
                                    .doubleValue() - (2 * difference)).max(new BigDecimal("0.0")),
                                    this.getUtilitySpace().getUtility(lastOfferedBid)));
                }

                // this should not be reached
                if (options.size().intValue() == 0) {
                    i = this.getRandom().nextInt(this.getBidspace().size().intValue());
                    return new Offer(this.getPartyId(), this.getBidspace().get(i));
                }

                i = this.getRandom().nextInt(options.size().intValue());

            }
            // don't concede
            else {
                options = this.getBidsWithinUtil()
                        .getBids(new Interval(this.getUtilitySpace().getUtility(lastOfferedBid), BigDecimal
                                .valueOf(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue() + difference)
                                .min(new BigDecimal("1.0"))));

                if (options.size().intValue() == 0) {
                    options = this.getBidsWithinUtil().getBids(new Interval(
                            this.getUtilitySpace().getUtility(lastOfferedBid),
                            BigDecimal.valueOf(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue() + 2*difference)
                            .min(new BigDecimal("1.0"))));
                }

                // this should not be reached
                if (options.size().intValue() == 0) {
                    i = this.getRandom().nextInt(this.getBidspace().size().intValue());
                    return new Offer(this.getPartyId(), this.getBidspace().get(i));
                }

                i = this.getRandom().nextInt(options.size().intValue());

            }
            Bid newBid = options.get(i);
            ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(
                    justLastOpponentBid, newBid)
                    ? new Offer(this.getPartyId(), newBid)
                    : new Accept(this.getPartyId(), justLastOpponentBid);
            return result;
        }
        // something else needed if the state is not a HistoryState
        return null;
    }

    public Bid extractBidFromAction(Action action) {
        Bid bid;
        if (action instanceof Offer) {
            bid = ((Offer) action).getBid();
        } else {
            bid = ((Accept) action).getBid();
        }
        return bid;
    }

    @JsonIgnore
    public BidsWithUtility getBidsWithinUtil() {
        return bidsWithinUtil;
    }

    @JsonIgnore
    public void setBidsWithinUtil(BidsWithUtility bidsWithinUtil) {
        this.bidsWithinUtil = bidsWithinUtil;
    }


}
