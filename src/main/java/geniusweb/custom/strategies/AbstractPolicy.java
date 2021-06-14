package geniusweb.custom.strategies;

import java.math.BigDecimal;
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

public abstract class AbstractPolicy {
    private UtilitySpace utilitySpace = null;
    private AllBidsList bidspace = null;
    private Domain domain;
    private String name = "DEFAULT";
    private PartyId partyId;

    private final Random random = new Random();

    public AbstractPolicy(Domain domain, String name) {
        super();
        this.setDomain(domain);
        this.setName(name);
        this.setPartyId(new PartyId("Opponent_" + name));
        Entry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>> preferencePairs = this
                .initRandomUtilityProfile(domain, name);
        this.utilitySpace = new LinearAdditiveUtilitySpace(domain, name, preferencePairs.getKey(),
                preferencePairs.getValue(), null);
        this.setBidspace(new AllBidsList(this.getDomain()));
    }
    public AbstractPolicy(Domain domain, String name, UtilitySpace utilitySpace, AllBidsList bidspace) {
        super();
        this.setDomain(domain);
        this.setName(name);
        this.setUtilitySpace(utilitySpace);
        this.setPartyId(new PartyId(name));
        this.setBidspace(new AllBidsList(this.getDomain()));
    }

    private Entry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>> initRandomUtilityProfile(Domain domain,
            String name) {
        List<String> issues = new ArrayList<String>(domain.getIssues());
        List<Integer> allInts = random.ints(issues.size()).boxed().collect(Collectors.toList()); // TODO: Bound the
                                                                                                 // random generation
        Integer sumOfInts = allInts.stream().mapToInt(Integer::intValue).sum();
        Map<String, BigDecimal> issueWeights = IntStream.range(0, issues.size()).boxed()
                .collect(Collectors.toMap(issues::get, index -> new BigDecimal(allInts.get(index) / sumOfInts)));

        HashMap<String, ValueSetUtilities> issueValueWeights = new HashMap<String, ValueSetUtilities>();
        for (String issueString : issues) {

            DiscreteValueSet values = (DiscreteValueSet) domain.getValues(issueString);
            // values.ge
            List<Long> randLongs = random.longs(values.size().longValue()).boxed().collect(Collectors.toList());
            Long sumOfValueLongs = randLongs.stream().mapToLong(Long::intValue).sum();
            Map<DiscreteValue, BigDecimal> valueWeights = LongStream.range(0, values.size().longValue()).boxed()
                    .collect(Collectors.toMap(values::get,
                            index -> new BigDecimal(randLongs.get(index.intValue()) / sumOfValueLongs)));
            issueValueWeights.put(issueString, new DiscreteValueSetUtilities(valueWeights));
        }

        return new AbstractMap.SimpleEntry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>>(
                issueValueWeights, issueWeights);
    }

    @Override
    public String toString() {
        return "PolicyName: " + getName() + " -- " + this.utilitySpace.toString();
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
    public abstract Action chooseAction(Bid lastAgentBid);
    public abstract Action chooseAction(Bid lastAgentBid, AbstractState<?> state);

}
