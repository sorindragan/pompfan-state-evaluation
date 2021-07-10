package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import geniusweb.actions.Offer;
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

public abstract class AbstractPolicy implements CommonOpponentInterface {
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

    public AbstractPolicy(UtilitySpace uSpace, String name) {
        super();
        this.setDomain(uSpace.getDomain());
        this.setName(name);
        this.setPartyId(new PartyId("Opponent_" + name));
        this.utilitySpace = uSpace;
        this.setBidspace(new AllBidsList(this.getDomain()));
    }

    private Entry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>> initRandomUtilityProfile(Domain domain,
            String name) {
        List<String> issues = new ArrayList<String>(domain.getIssues());
        List<Integer> allInts = random.ints(issues.size(), 0, 100).boxed().collect(Collectors.toList()); // TODO: Bound
                                                                                                         // the
        // random generation
        MathContext mc = new MathContext(5);
        Integer sumOfInts = allInts.stream().mapToInt(Integer::intValue).sum();
        Map<String, BigDecimal> issueWeights = IntStream.range(0, issues.size()).boxed().collect(
                Collectors.toMap(issues::get, index -> new BigDecimal((double) allInts.get(index) / sumOfInts, mc)));

        // To make everything add to 1
        Double remainder = 1 - issueWeights.values().stream().mapToDouble(bigD -> bigD.doubleValue()).sum();
        String firstKey = (String) issueWeights.keySet().toArray()[0];
        issueWeights.computeIfPresent(firstKey, (key, value) -> value.add(new BigDecimal(remainder, mc)));

        HashMap<String, ValueSetUtilities> issueValueWeights = new HashMap<String, ValueSetUtilities>();
        for (String issueString : issues) {

            DiscreteValueSet values = (DiscreteValueSet) domain.getValues(issueString);
            List<Long> randLongs = random.longs(values.size().longValue(), 0, 100).boxed().collect(Collectors.toList());
            Long sumOfValueLongs = randLongs.stream().mapToLong(Long::intValue).sum();
            Map<DiscreteValue, BigDecimal> valueWeights = LongStream.range(0, values.size().longValue()).boxed()
                    .collect(Collectors.toMap(values::get,
                            index -> new BigDecimal((double) randLongs.get(index.intValue()) / sumOfValueLongs, mc)));
            // To make everything add to 1
            Double valueRemainder = 1 - valueWeights.values().stream().mapToDouble(bigD -> bigD.doubleValue()).sum();
            DiscreteValue firstValueKey = (DiscreteValue) valueWeights.keySet().toArray()[0];
            valueWeights.computeIfPresent(firstValueKey, (key, value) -> value.add(new BigDecimal(valueRemainder, mc)));

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

    @Override
    public Action chooseAction() {
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid);
    }

    @Override
    public Action chooseAction(AbstractState<?> state) {
        return this.chooseAction();
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(lastAgentBid, state);
    }

}
