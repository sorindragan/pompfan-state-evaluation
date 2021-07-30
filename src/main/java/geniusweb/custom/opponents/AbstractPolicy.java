package geniusweb.custom.opponents;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

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

// @JsonSubTypes({ @Type(value = AntagonisticOpponentPolicy.class),
//         @Type(value = RandomOpponentPolicy.class),
//         @Type(value = SelfishOpponentPolicy.class),
//         @Type(value = TimeDependentOpponentPolicy.class) })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = AntagonisticOpponentPolicy.class), @Type(value = RandomOpponentPolicy.class),
        @Type(value = SelfishOpponentPolicy.class), @Type(value = TimeDependentOpponentPolicy.class) })
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class AbstractPolicy implements CommonOpponentInterface, Serializable {
    private String id = UUID.randomUUID().toString();
    private UtilitySpace utilitySpace = null;
    @JsonIgnore
    private AllBidsList bidspace = null;
    private Domain domain;
    private String name = "DEFAULT-ABSTRACT-POLICY";
    private PartyId partyId;

    @JsonIgnore
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
        // this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private Entry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>> initRandomUtilityProfile(Domain domain,
            String name) {
        List<String> issues = new ArrayList<String>(domain.getIssues());
        List<BigDecimal> allInts = random.ints(issues.size(), 0, 100).boxed().map(String::valueOf).map(BigDecimal::new)
                .collect(Collectors.toList());
        // MathContext mc = new MathContext(5);
        BigDecimal sumOfInts = allInts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> issueWeights = IntStream.range(0, issues.size()).boxed().collect(
                Collectors.toMap(issues::get, index -> allInts.get(index).divide(sumOfInts, 5, RoundingMode.HALF_UP)));

        // To make everything add to 1
        BigDecimal remainder = BigDecimal.ONE
                .subtract(issueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        String firstKey = (String) issueWeights.keySet().toArray()[0];
        issueWeights.computeIfPresent(firstKey, (key, value) -> value.add(remainder));

        HashMap<String, ValueSetUtilities> issueValueWeights = new HashMap<String, ValueSetUtilities>();
        for (String issueString : issues) {

            DiscreteValueSet values = (DiscreteValueSet) domain.getValues(issueString);
            List<BigDecimal> randLongs = random.longs(values.size().longValue(), 1, 100).boxed().map(String::valueOf)
                    .map(BigDecimal::new).collect(Collectors.toList());
            BigDecimal sumOfValueLongs = randLongs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<DiscreteValue, BigDecimal> valueWeights = LongStream.range(0, values.size().longValue()).boxed()
                    .collect(Collectors.toMap(values::get,
                            index -> randLongs.get(index.intValue()).divide(sumOfValueLongs, 5, RoundingMode.HALF_UP)));
            // To make everything add to 1
            BigDecimal valueRemainder = BigDecimal.ONE
                    .subtract(valueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            DiscreteValue firstValueKey = (DiscreteValue) valueWeights.keySet().toArray()[valueWeights.keySet().size()
                    - 1];
            valueWeights.computeIfPresent(firstValueKey, (key, value) -> value.add(valueRemainder));
            // System.out.println("valueWeights");
            // System.out.println(valueWeights);
            // System.out.println(valueWeights.values().stream().reduce(BigDecimal.ZERO,
            // BigDecimal::add));
            // try {
            // new DiscreteValueSetUtilities(valueWeights);
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
            issueValueWeights.put(issueString, new DiscreteValueSetUtilities(valueWeights));
        }

        return new AbstractMap.SimpleEntry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>>(
                issueValueWeights, issueWeights);
    }

    @Override
    public String toString() {
        // return "PolicyName: " + getName() + " -- " + this.utilitySpace.toString();
        return this.getId().toString();
    }

    public AllBidsList getBidspace() {
        return bidspace;
    }

    public AbstractPolicy setBidspace(AllBidsList bidspace) {
        this.bidspace = bidspace;
        return this;
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
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

}
