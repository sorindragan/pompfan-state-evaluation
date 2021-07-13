package geniusweb.custom.explorers;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import geniusweb.actions.Action;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.DiscreteValueSet;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profile.utilityspace.ValueSetUtilities;

public abstract class AbstractOwnExplorationPolicy {
    private UtilitySpace utilitySpace = null;
    private AllBidsList bidspace = null;
    private Domain domain;
    private String name = "DEFAULT";
    private PartyId partyId;

    private final Random random = new Random();


    public AbstractOwnExplorationPolicy(Domain domain, String name, UtilitySpace utilitySpace, PartyId id) {
        super();
        this.setDomain(domain);
        this.setName(name);
        this.setUtilitySpace(utilitySpace);
        this.setPartyId(id);
        this.setBidspace(new AllBidsList(domain));
    }

    @Override
    public String toString() {
        return "On behalf of " + getPartyId().getName() + ": " + getName() + " -- " + this.utilitySpace.toString();
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

    public abstract Action chooseAction();

    public Action chooseAction(AbstractState<?> state) {
        return this.chooseAction();
    }

    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }
}
