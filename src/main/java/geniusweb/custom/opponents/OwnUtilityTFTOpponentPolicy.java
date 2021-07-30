package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.HistoryState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilityTFTOpponentPolicy extends AbstractPolicy {

    private static double utilGapForConcession = 0.02;
    BidsWithUtility bidsWithinUtil;

    public OwnUtilityTFTOpponentPolicy(UtilitySpace uSpace) {
        super(uSpace, "OwnUtilTFT");
        // TODO Auto-generated constructor stub
        this.setBidsWithinUtil(new BidsWithUtility((LinearAdditive) this.getUtilitySpace()));
    }

    @Override
    public Action chooseAction(AbstractState<?> state) {
        boolean isConcession = false;
        if (state instanceof HistoryState) {
            double difference = 0.0;
            ArrayList<Bid> bidHistory = new ArrayList<>(((HistoryState) state).getHistory().stream()
                    .map(this::extractBidFromAction).collect(Collectors.toList()));
            int historySize = bidHistory.size();
            if (historySize > 3) {
                Bid lastLastOpponentBid = bidHistory.get(historySize - 3);
                Bid justLastOpponentBid = bidHistory.get(historySize - 1);
                difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                        .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid)).doubleValue();
                isConcession = difference > utilGapForConcession ? true : false;
            } else {
                isConcession = true;
            }

            Bid lastOfferedBid = bidHistory.get(historySize - 2);
            ImmutableList<Bid> options;
            long i;
            // also concede
            if (isConcession) {
                // I need something like getBidNearUtility ffs
                // I did a computationally-expensive ugly workaround
                options = this.getBidsWithinUtil()
                    .getBids(new Interval(
                            new BigDecimal(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue() - difference - utilGapForConcession).
                            min(new BigDecimal("0.0")), this.getUtilitySpace().getUtility(lastOfferedBid)
                        ));
                // TODO: check if options is empty
                i = this.getRandom().nextInt(options.size().intValue());
                
            }
            // don't concede
            else {
                options = this.getBidsWithinUtil()
                    .getBids(new Interval(this.getUtilitySpace().getUtility(lastOfferedBid),
                             new BigDecimal(this.getUtilitySpace().getUtility(lastOfferedBid).doubleValue() + difference + utilGapForConcession)
                                       .max(this.getUtilitySpace().getUtility(this.getBidsWithinUtil().getExtremeBid(true)))
                        ));
                // TODO: check if options is empty
                i = this.getRandom().nextInt(options.size().intValue());
                
            }
            Bid newBid = options.get(i);
            return new Offer(this.getPartyId(), newBid);
        }
        // TODO: see what other shit is needed

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

    public BidsWithUtility getBidsWithinUtil() {
        return bidsWithinUtil;
    }

    public void setBidsWithinUtil(BidsWithUtility bidsWithinUtil) {
        this.bidsWithinUtil = bidsWithinUtil;
    }

}
