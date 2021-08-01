package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.custom.opponentModels.AbstractOpponentModel;
import geniusweb.custom.opponentModels.WeightedFrequencyOpponentModel;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.profile.utilityspace.LinearAdditive;
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
    private AbstractOpponentModel oppModel = null;

    private final BigDecimal STUBBORNESS = new BigDecimal(new Random().nextDouble());

    public SimpleOpponentModelPolicy(Domain domain, String name, List<Action> realHistoryActions) {
        super(domain, name);
        this.setUtilitySpace(new WeightedFrequencyOpponentModel(domain, realHistoryActions));
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

    public AbstractOpponentModel getOppModel() {
        return oppModel;
    }

    public void setOppModel(AbstractOpponentModel oppModel) {
        this.oppModel = oppModel;
    }

}
