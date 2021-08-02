package geniusweb.pompfan.explorers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.opponents.CommonOpponentInterface;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public abstract class AbstractOwnExplorationPolicy{
    private UtilitySpace utilitySpace = null;
    private AllBidsList bidspace = null;
    private Domain domain;
    private String name = "DEFAULT";
    private PartyId partyId;

    private final Random random = new Random();
    private BidsWithUtility allBids;
    private Interval possibleRange;
    private Interval searchRange;
    private BigDecimal stubborness = new BigDecimal(0.5);
    private ImmutableList<Bid> possibleBids;
    private BigDecimal lowerBound;
    private BigDecimal upperBound;

    public AbstractOwnExplorationPolicy(UtilitySpace utilitySpace, PartyId id){
        super();
        this.setUtilitySpace(utilitySpace);
        this.setName(this.getClass().getSimpleName());
        this.setPartyId(id);
        this.setDomain(this.getUtilitySpace().getDomain());
        this.setBidspace(new AllBidsList(utilitySpace.getDomain()));
        this.setAllBids(new BidsWithUtility((LinearAdditive) this.getUtilitySpace()));
        this.setPossibleRange(this.getAllBids().getRange());
        this.setLowerBound(this.getPossibleRange().getMax().multiply(stubborness));
        this.setUpperBound(this.getPossibleRange().getMax());
        this.setSearchRange(new Interval(this.getLowerBound(), this.getUpperBound()));
        this.setPossibleBids(this.getAllBids().getBids(this.getSearchRange()));
        this.init();
    }

    public BigDecimal getSTUBBORNESS() {
        return stubborness;
    }

    public void setSTUBBORNESS(BigDecimal sTUBBORNESS) {
        stubborness = sTUBBORNESS;
    }

    public BigDecimal getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(BigDecimal lowerBound) {
        this.lowerBound = lowerBound;
    }

    public BigDecimal getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(BigDecimal upperBound) {
        this.upperBound = upperBound;
    }

    public ImmutableList<Bid> getPossibleBids() {
        return possibleBids;
    }

    public void setPossibleBids(ImmutableList<Bid> possibleBids) {
        this.possibleBids = possibleBids;
    }

    public Interval getSearchRange() {
        return searchRange;
    }

    public void setSearchRange(Interval searchRange) {
        this.searchRange = searchRange;
    }

    public Interval getPossibleRange() {
        return possibleRange;
    }

    public void setPossibleRange(Interval possibleRange) {
        this.possibleRange = possibleRange;
    }

    public BidsWithUtility getAllBids() {
        return allBids;
    }

    public void setAllBids(BidsWithUtility allBids) {
        this.allBids = allBids;
    }

    public AllBidsList getBidspace() {
        return bidspace;
    }

    public void setBidspace(AllBidsList bidspace) {
        this.bidspace = bidspace;
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Random getRandom() {
        return random;
    }

    public PartyId getPartyId() {
        return partyId;
    }

    public void setPartyId(PartyId partyId) {
        this.partyId = partyId;
    }

    public Action chooseAction() {
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid);
    }

    public Action chooseAction(AbstractState<?> state) {
        return this.chooseAction();
    }

    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    @Override
    public String toString() {
        return "On behalf of " + getPartyId().getName() + ": " + getName() + " -- " + this.utilitySpace.toString();
    }

    protected abstract void init();
}
