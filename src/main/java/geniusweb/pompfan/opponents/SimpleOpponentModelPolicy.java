package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

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
import geniusweb.pompfan.opponentModels.AbstractOpponentModel;
import geniusweb.pompfan.opponentModels.EntropyWeightedOpponentModel;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class SimpleOpponentModelPolicy extends AbstractPolicy {
    @JsonIgnore
    private BidsWithUtility allBids;
    @JsonIgnore
    private Interval possibleRange;
    @JsonIgnore
    private Interval searchRange;
    @JsonIgnore
    private ImmutableList<Bid> possibleBids;
    @JsonIgnore
    private AbstractOpponentModel oppModel = null;

    public BigDecimal STUBBORNESS = new BigDecimal(new Random().nextDouble());
    private List<Action> recordedBehavior;

    @JsonCreator
    public SimpleOpponentModelPolicy(@JsonProperty("domain") Domain domain, @JsonProperty("name") String name,
            @JsonProperty("recordedBehavior") List<Action> recordedBehavior,
            @JsonProperty("STUBBORNESS") BigDecimal STUBBORNESS) {
        super(new EntropyWeightedOpponentModel(domain, recordedBehavior), name);
        this.setRecordedBehavior(recordedBehavior);
        this.allBids = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.possibleRange = this.getAllBids().getRange();
        this.searchRange = new Interval(this.getPossibleRange().getMax().multiply(STUBBORNESS),
                this.getPossibleRange().getMax());
        this.possibleBids = this.getAllBids().getBids(this.getSearchRange());
    }

    public SimpleOpponentModelPolicy(Domain domain, String name, List<Action> realHistoryActions) {
        super(domain, name);
        this.setUtilitySpace(new EntropyWeightedOpponentModel(domain, realHistoryActions));
        this.setRecordedBehavior(realHistoryActions);
        this.allBids = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        this.possibleRange = this.getAllBids().getRange();
        this.searchRange = new Interval(this.getPossibleRange().getMax().multiply(STUBBORNESS),
                this.getPossibleRange().getMax());
        this.possibleBids = this.getAllBids().getBids(this.getSearchRange());
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    // DONE: refactor others
    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {

        if (this.getPossibleBids().size().compareTo(BigInteger.ZERO) == 0) {
            return this.chooseAction();
        }
        if (lastReceivedBid == null) {
            return pickAcceptableBid();
        }
        if (isGood(lastReceivedBid)) {
            return new Accept(this.getPartyId(), lastReceivedBid);
        }
        return pickAcceptableBid();
    }

    private Action pickAcceptableBid() {
        long i = this.getRandom().nextInt(this.possibleBids.size().intValue());
        Bid bid = this.getPossibleBids().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid);
    }

    private boolean isGood(Bid bid) {
        if (STUBBORNESS.compareTo(this.getUtilitySpace().getUtility(bid)) < 0) {
            return true;
        }
        return false;
    }

    public BidsWithUtility getAllBids() {
        return allBids;
    }

    public void setAllBids(BidsWithUtility allBids) {
        this.allBids = allBids;
    }

    public Interval getPossibleRange() {
        return possibleRange;
    }

    public void setPossibleRange(Interval possibleRange) {
        this.possibleRange = possibleRange;
    }

    public Interval getSearchRange() {
        return searchRange;
    }

    public void setSearchRange(Interval searchRange) {
        this.searchRange = searchRange;
    }

    public ImmutableList<Bid> getPossibleBids() {
        return possibleBids;
    }

    public void setPossibleBids(ImmutableList<Bid> possibleBids) {
        this.possibleBids = possibleBids;
    }

    @JsonIgnore
    public AbstractOpponentModel getOppModel() {
        return oppModel;
    }

    @JsonIgnore
    public void setOppModel(AbstractOpponentModel oppModel) {
        this.oppModel = oppModel;
    }

    public List<Action> getRecordedBehavior() {
        return recordedBehavior;
    }

    public void setRecordedBehavior(List<Action> recordedBehavior) {
        this.recordedBehavior = recordedBehavior;
    }

    @JsonIgnore
    @Override
    public void setUtilitySpace(UtilitySpace utilitySpace) {
        super.setUtilitySpace(utilitySpace);
    }

    @JsonIgnore
    @Override
    public UtilitySpace getUtilitySpace() {
        return super.getUtilitySpace();
    }
}
