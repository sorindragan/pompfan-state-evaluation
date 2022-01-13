package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
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
import geniusweb.actions.PartyId;
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
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import tudelft.utilities.immutablelist.ImmutableList;

public class OppUtilityTFTOpponentPolicy extends AbstractPolicy {

    // private static double utilGapForConcession = 0.02;
    // private List<Bid> oppBadBids;
    @JsonIgnore
    BidsWithUtility bidsWithinUtil;
    private AbstractOpponentModel opponentModel = null;
    private Bid myLastbid = null;
    private BidsWithUtility oppBidsWithUtilities;
    private final boolean DEBUG = false;
    private Progress progress;

    
    @JsonCreator
    public OppUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, @JsonProperty("name") String name, Progress progress) {
        super(uSpace, name);
        this.progress = progress;
    }
    
    public OppUtilityTFTOpponentPolicy(Domain domain, Progress progress) {
        super(domain, "OppUtilTFT");
        this.progress = progress;

    }
    
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        
        ActionWithBid action;
        ArrayList<ActionWithBid> bidHistory = new ArrayList<ActionWithBid>(Arrays.asList(
                new Offer(new PartyId("Opp"), second2lastReceivedBid),
                new Offer(this.getPartyId(), lastOwnBid),
                new Offer(new PartyId("Opp"), lastReceivedBid)
                ));
        
        myLastbid = lastOwnBid;
        int historySize = bidHistory.size();

        if (historySize > 3) {
            this.updateOpponentModel(bidHistory, state);
        }
        if (this.opponentModel == null) {
            Bid bid = new BidsWithUtility((LinearAdditiveUtilitySpace) this.getUtilitySpace()).getExtremeBid(true);
            action = new Offer(this.getPartyId(), bid);
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = bidHistory.get(historySize - 3).getBid();
        Bid justLastOpponentBid = bidHistory.get(historySize - 1).getBid();

        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal opponentUtilityOfmyLastOwnBid = this.opponentModel.getUtility(myLastbid);

        BigDecimal utilityGoal = isConcession
                ? opponentUtilityOfmyLastOwnBid.add(difference.abs())
                        .min(BigDecimal.ONE)
                : opponentUtilityOfmyLastOwnBid.subtract(difference.abs())
                        .max(new BigDecimal("0.2"));

        Bid selectedBid = computeNextBid(utilityGoal);
        double time = state.getTime();

        if (DEBUG) {
            System.out.println("ppppppppppppppppppppppppppp");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility-For-Opp: " + this.opponentModel.getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal-For-Opp: " + utilityGoal);
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
        return super.chooseAction(state);              
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {
        ImmutableList<Bid> options = this.oppBidsWithUtilities.getBids(
                new Interval(utilityGoal.subtract(new BigDecimal("0.2")), utilityGoal));
        if (options.size().intValue() < 1) {
            options = this.oppBidsWithUtilities.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.2")), 
                            utilityGoal.add(new BigDecimal("0.2"))));
        }
        
        try {
            // this is already crazy
            return options.get(options.size().intValue() - 1);
        } catch (Exception e) {
            System.out.println("PARTICLE: OM Faild to properly capture the utility space. " + utilityGoal.doubleValue());
            options = this.oppBidsWithUtilities.getBids(
                    new Interval(utilityGoal.subtract(new BigDecimal("0.5")).max(BigDecimal.ZERO),
                            utilityGoal.add(new BigDecimal("0.5")).min(BigDecimal.ONE)));
            return options.get(options.size().intValue() - 1);
        }
    }

    private void updateOpponentModel(List<ActionWithBid> history, AbstractState<?> state) {
        this.opponentModel = new BetterFreqOppModel(this.getUtilitySpace().getDomain(),
                history.stream().map(a -> (Action) a).collect(Collectors.toList()), this.getPartyId(), this.progress);
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModel);
    }

}
