package geniusweb.pompfan.particles;

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
import geniusweb.exampleparties.anac2021.tripleagent.BidHistory;
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

    @JsonIgnore
    BidsWithUtility bidsWithinUtil;
    private AbstractOpponentModel opponentModelBelief = null;
    private AbstractOpponentModel opponentModelParticle = null;
    private Bid myLastbid = null;
    private BidsWithUtility oppBidsWithUtilities;
    private final boolean DEBUG = false;
    private Progress progress;
    private ArrayList<ActionWithBid> bidHistory = null;
    
    @JsonCreator
    public OppUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, @JsonProperty("name") String name, Progress progress) {
        super(uSpace, name);
        this.progress = progress;
    }
    
    public OppUtilityTFTOpponentPolicy(Domain domain, Progress progress) {
        super(domain, "OppUtilTFT");
        this.progress = progress;

    }
    
    // used in the belief update
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        
        ActionWithBid action;
        if (bidHistory == null) {
            bidHistory = new ArrayList<ActionWithBid>(Arrays.asList(
                    new Offer(new PartyId("Opp"), second2lastReceivedBid),
                    new Offer(this.getPartyId(), lastOwnBid),
                    new Offer(new PartyId("Opp"), lastReceivedBid)
                    ));
        } else {
            bidHistory.add(new Offer(this.getPartyId(), lastOwnBid));
            bidHistory.add( new Offer(new PartyId("Opp"), lastReceivedBid));
        }
        
        int historySize = bidHistory.size();

        if (historySize > 4) {
            this.updateBeliefOpponentModel(bidHistory, state);
        }

        if (this.opponentModelBelief == null) {
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

        BigDecimal opponentUtilityOfmyLastOwnBid = this.opponentModelBelief.getUtility(lastOwnBid);

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
            System.out.println("TFT-Last-Utility-For-Opp: " + this.opponentModelBelief.getUtility(lastOwnBid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal-For-Opp: " + utilityGoal);
        }

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                && time > 0.8
                        ? new Accept(this.getPartyId(), justLastOpponentBid)
                        : new Offer(this.getPartyId(), selectedBid);
        
    }

    // used in constructing the tree
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        ActionWithBid action;
        ArrayList<ActionWithBid> simulatedHistory = new ArrayList<>(((HistoryState) state).getHistory().stream()
                .map(b -> ((ActionWithBid) b)).collect(Collectors.toList()));

        int historySize = simulatedHistory.size();

        if (historySize > 4) {
            this.updateParticleOpponentModel(simulatedHistory, state);
        }
        
        if (this.opponentModelParticle == null) {
            Bid bid = new BidsWithUtility((LinearAdditiveUtilitySpace) this.getUtilitySpace()).getExtremeBid(true);
            action = new Offer(this.getPartyId(), bid);
            myLastbid = bid;
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = simulatedHistory.get(historySize - 3).getBid();
        Bid justLastOpponentBid = simulatedHistory.get(historySize - 1).getBid();

        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal opponentUtilityOfmyLastOwnBid = this.opponentModelParticle.getUtility(myLastbid);

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
            System.out.println("TFT-Last-Utility-For-Opp: " + this.opponentModelParticle.getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal-For-Opp: " + utilityGoal);
        }
        myLastbid = selectedBid;

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                && time > 0.8
                        ? new Accept(this.getPartyId(), justLastOpponentBid)
                        : new Offer(this.getPartyId(), selectedBid);
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
            // ! when same bids are made by the opponent in the history of the state
            // ! this fails miserably
            return options.get(options.size().intValue() - 1);
        }
    }

    private void updateBeliefOpponentModel(List<ActionWithBid> history, AbstractState<?> state) {
        this.opponentModelBelief = new BetterFreqOppModel(this.getUtilitySpace().getDomain(),
                history.stream().map(a -> (Action) a).collect(Collectors.toList()), this.getPartyId(), this.progress);
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModelBelief);
    }

    private void updateParticleOpponentModel(List<ActionWithBid> history, AbstractState<?> state) {
        this.opponentModelParticle = new BetterFreqOppModel(this.getUtilitySpace().getDomain(),
                history.stream().map(a -> (Action) a).collect(Collectors.toList()), this.getPartyId(), this.progress);
        this.oppBidsWithUtilities = new BidsWithUtility(this.opponentModelParticle);
    }

}
