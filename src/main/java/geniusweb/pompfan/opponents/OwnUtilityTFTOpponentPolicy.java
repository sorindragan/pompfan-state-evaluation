package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilityTFTOpponentPolicy extends AbstractPolicy {

    private static double utilGapForConcession = 0.02;
    @JsonIgnore
    BidsWithUtility bidsWithinUtil;

    
    @JsonCreator
    public OwnUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace) {
        super(uSpace, "OwnUtilTFT");
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
        if (state instanceof HistoryState) {
            double difference = 0.0;
            ArrayList<Bid> bidHistory = new ArrayList<>(((HistoryState) state).getHistory().stream()
                    .map(this::extractBidFromAction).collect(Collectors.toList()));
            int historySize = bidHistory.size();
            Bid lastOfferedBid = this.getBidsWithinUtil().getExtremeBid(true);
            if (historySize > 3) {
                Bid lastLastOpponentBid = bidHistory.get(historySize - 3);
                Bid justLastOpponentBid = bidHistory.get(historySize - 1);
                difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                        .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid)).doubleValue();
                isConcession = difference > utilGapForConcession ? true : false;
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
                                        - difference - utilGapForConcession).min(new BigDecimal("0.0")),
                                this.getUtilitySpace().getUtility(lastOfferedBid)));

                if (options.size().intValue() == 0) {
                    options = this.getBidsWithinUtil()
                            .getBids(
                                    new Interval(
                                            BigDecimal
                                                    .valueOf(this.getUtilitySpace().getUtility(lastOfferedBid)
                                                            .doubleValue() - 2 * (difference + utilGapForConcession))
                                                    .min(new BigDecimal("0.0")),
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
                                .valueOf(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue() + difference
                                        + utilGapForConcession)
                                .max(this.getUtilitySpace().getUtility(this.getBidsWithinUtil().getExtremeBid(true)))));

                if (options.size().intValue() == 0) {
                    options = this.getBidsWithinUtil().getBids(new Interval(
                            this.getUtilitySpace().getUtility(lastOfferedBid),
                            this.getUtilitySpace().getUtility(lastOfferedBid).max(
                                    this.getUtilitySpace().getUtility(this.getBidsWithinUtil().getExtremeBid(true)))));
                }

                // this should not be reached
                if (options.size().intValue() == 0) {
                    i = this.getRandom().nextInt(this.getBidspace().size().intValue());
                    return new Offer(this.getPartyId(), this.getBidspace().get(i));
                }

                i = this.getRandom().nextInt(options.size().intValue());

            }
            Bid newBid = options.get(i);
            return new Offer(this.getPartyId(), newBid);
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
