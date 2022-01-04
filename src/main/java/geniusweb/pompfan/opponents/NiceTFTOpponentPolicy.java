package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import geniusweb.pompfan.opponentModels.AHPFreqWeightedOpponentModel;
import geniusweb.pompfan.opponentModels.AbstractOpponentModel;
import geniusweb.pompfan.opponentModels.BetterFreqOppModel;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class NiceTFTOpponentPolicy extends AbstractPolicy {

    // private static double utilGapForConcession = 0.02;
    // private List<Bid> oppBadBids;
    @JsonIgnore
    BidsWithUtility bidsWithinUtil;
    private AbstractOpponentModel opponentModel;
    private BidsWithUtility oppBidsWithUtilities;
    private AllBidsList allPossibleBids;

    
    @JsonCreator
    public NiceTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, @JsonProperty("name") String name) {
        super(uSpace, name);
        this.setBidsWithinUtil(new BidsWithUtility((LinearAdditive) this.getUtilitySpace()));
    }
    
    public NiceTFTOpponentPolicy(Domain domain) {
        super(domain, "OppUtilTFT");
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
        ActionWithBid action;
        if (state instanceof HistoryState) {
            ArrayList<ActionWithBid> bidHistory = new ArrayList<>(((HistoryState) state).getHistory().stream()
                    .map(b -> ((ActionWithBid) b)).collect(Collectors.toList()));
            int historySize = bidHistory.size();
            
            if (historySize > 6) {
                // List<ActionWithBid> oppHistory = new ArrayList<>();
                this.updateOpponentModel(bidHistory);
            }
        

            if (this.allPossibleBids == null) {
                this.allPossibleBids = new AllBidsList(this.getUtilitySpace().getDomain());
            }
        
            if (this.opponentModel == null) {
                
                Bid bid = this.getBidsWithinUtil().getExtremeBid(true);
                action = new Offer(this.getPartyId(), bid);
                return action;
            }
            boolean isConcession = false;
            
            Bid lastLastOpponentBid = bidHistory.get(historySize - 3).getBid();
            Bid justLastOpponentBid = bidHistory.get(historySize - 1).getBid();
            
            BigDecimal difference = BigDecimal.ZERO;
            BigDecimal cntBidSpace = new BigDecimal(this.allPossibleBids.size());
            BigDecimal concessionThreshold = BigDecimal.ONE.divide(cntBidSpace, 5, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(2));
            difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                    .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
            isConcession = difference.compareTo(concessionThreshold) >= 0 ? true : false;

            Bid lastOwnBid = bidHistory.get(historySize - 2).getBid();
            BigDecimal utilityForOpponentOfLastOwnBid = this.opponentModel.getUtility(lastOwnBid);
            Interval fullRange = this.oppBidsWithUtilities.getRange();
            Interval newInterval = new Interval(this.getUtilitySpace().getUtility(lastOwnBid),
                    this.getBidsWithinUtil().getRange().getMax());
            // in case of no concession
            ImmutableList<Bid> candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
            if (isConcession) {
                newInterval = new Interval(utilityForOpponentOfLastOwnBid,
                        utilityForOpponentOfLastOwnBid.add(difference.multiply(BigDecimal.valueOf(2))).min(BigDecimal.ONE));
                candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
                if (candiateBids.size().compareTo(BigInteger.ZERO) <= 0) {
                    newInterval = new Interval(utilityForOpponentOfLastOwnBid, utilityForOpponentOfLastOwnBid
                            .add(difference.multiply(BigDecimal.valueOf(2))).min(BigDecimal.ONE));
                }
                candiateBids = this.oppBidsWithUtilities.getBids(newInterval);
                if (candiateBids.size().compareTo(BigInteger.ZERO) <= 0) {
                    newInterval = fullRange;
                }

            }
            // safety behaviour in case of unexpected behaviour
            if (candiateBids.size().compareTo(BigInteger.ZERO) == 0) {
                Bid bid = this.getBidsWithinUtil().getExtremeBid(true);
                action = new Offer(this.getPartyId(), bid);
                return action;
            }

            int selectedIdx = this.getRandom().nextInt(candiateBids.size().intValue());
            Bid selectedBid = candiateBids.get(selectedIdx);
            ActionWithBid result = this.getUtilitySpace().isPreferredOrEqual(selectedBid, justLastOpponentBid)
                    ? new Offer(this.getPartyId(), selectedBid)
                    : new Accept(this.getPartyId(), justLastOpponentBid);
            return result;
        }
        // not implemented for non-History states
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

    private void updateOpponentModel(List<ActionWithBid> history) {
        this.opponentModel = new BetterFreqOppModel(this.getUtilitySpace().getDomain(),
                history.stream().map(a -> (Action) a).collect(Collectors.toList()), this.getPartyId());
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
        // maybe used for other opponent types
        // BigDecimal minUtility = this.opponentModel.getUtility(this.oppBidsWithUtilities.getExtremeBid(false));
        // BigDecimal minUtilityUpperBound = minUtility.multiply(new BigDecimal("1.5")).min(BigDecimal.ONE);
        // this.oppBadBids = StreamSupport
        //         .stream(this.oppBidsWithUtilities.getBids(new Interval(minUtility, minUtilityUpperBound)).spliterator(),
        //                 true)
        //         .collect(Collectors.toList());
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
