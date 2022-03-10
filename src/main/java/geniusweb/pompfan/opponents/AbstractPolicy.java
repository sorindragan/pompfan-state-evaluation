package geniusweb.pompfan.opponents;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.DiscreteValueSet;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import javafx.util.Pair;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = AntagonisticOpponentPolicy.class), @Type(value = RandomOpponentPolicy.class),
        @Type(value = SelfishOpponentPolicy.class), @Type(value = TimeDependentOpponentPolicy.class),
        @Type(value = SimpleOpponentModelPolicy.class), @Type(value = ImitateOpponentPolicy.class),
        @Type(value = OwnUtilityTFTOpponentPolicy.class) })
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public abstract class AbstractPolicy implements CommonOpponentInterface, Serializable {
    private String id = UUID.randomUUID().toString();
    private UtilitySpace utilitySpace = null;
    @JsonIgnore
    private AllBidsList bidspace = null;
    private Domain domain;
    private String name = "DEFAULT-ABSTRACT-POLICY";
    private PartyId partyId;

    // necessary for the fast bid search
    private HashMap<BigDecimal, LinkedList<Bid>> bidsHash;
    private ArrayList<BigDecimal> sortedUtilKeys;
    public Pair<Bid, BigDecimal> minBidWithUtil;
    public Pair<Bid, BigDecimal> maxBidWithUtil;

    @JsonIgnore
    private Random random = null;

    public AbstractPolicy(Domain domain, String name) {
        super();
        this.setDomain(domain);
        this.setName(name);
        this.setPartyId(new PartyId("Simulated_" + name));
        this.setBidspace(new AllBidsList(this.getDomain()));
        this.setRandom(new Random());
        Entry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>> preferencePairs = this
                .initRandomUtilityProfile(domain, name);
        this.setUtilitySpace(new LinearAdditiveUtilitySpace(domain, name, preferencePairs.getKey(),
                preferencePairs.getValue(), null));
        int bidsNumber = this.bidspace.size().intValue();
        System.out.println("Creting a new particle");
        this.bidsHash = new HashMap<BigDecimal, LinkedList<Bid>>(bidsNumber);
        this.sortedUtilKeys = new ArrayList<BigDecimal>(bidsNumber);
        AllBidsList currentBidSpace = this.getBidspace();
        BigDecimal currUtil;
        System.out.println(bidsNumber);
        double secondsInNano = 1000000000.0;
        long start = System.nanoTime();

        // init an array for each key as there might be multiple bids with same utility
        // for (long i=0; i < currentBidSpace.size().longValue(); i++) {
        //     currUtil = this.getUtilitySpace().getUtility(currentBidSpace.get(i));
        //     this.bidsHash.put(currUtil, new ArrayList<Bid>());
        //     this.sortedUtilKeys.add(currUtil);
        // }
        
        // this can be sped up by losing bids; aka use a value instead of an arraylist of values
        // the used precision is 10
        long listSize = currentBidSpace.size().longValue()-1;
        for (long i=0; i < currentBidSpace.size().longValue() / 2; i++) {
            currUtil = this.getUtilitySpace().getUtility(currentBidSpace.get(i));
            this.bidsHash.put(currUtil, new LinkedList<Bid>());
            currUtil = this.getUtilitySpace().getUtility(currentBidSpace.get(listSize-i));
            this.bidsHash.put(currUtil, new LinkedList<Bid>());
        }
        currUtil = this.getUtilitySpace().getUtility(currentBidSpace.get(listSize / 2));
        this.bidsHash.put(currUtil, new LinkedList<Bid>());

        long finish = System.nanoTime();
        long timeElapsed = finish - start;
        System.out.println("Initializing the hash took: " + timeElapsed / secondsInNano);

        start = System.nanoTime();
        Bid currentBid;
        for (long i = 0; i < currentBidSpace.size().longValue(); i++) {
            currentBid = currentBidSpace.get(i);
            currUtil = this.getUtilitySpace().getUtility(currentBid);
            this.bidsHash.get(currUtil).add(currentBid);
            this.sortedUtilKeys.add(currUtil);
        }
        finish = System.nanoTime();
        timeElapsed = finish - start;
        System.out.println("Populating the hash took: " + timeElapsed / secondsInNano);
        
        start = System.nanoTime();
        this.sortedUtilKeys.sort(new Comparator<BigDecimal>() {
            @Override
            public int compare(BigDecimal o1, BigDecimal o2) {
                return o1.compareTo(o2);
            }
        });
        finish = System.nanoTime();
        timeElapsed = finish - start;
        System.out.println("Sorting took: " + timeElapsed / secondsInNano);

        this.minBidWithUtil = new Pair<Bid, BigDecimal>(this.bidsHash.get(this.sortedUtilKeys.get(0)).get(0),
                this.sortedUtilKeys.get(0));
        this.maxBidWithUtil = new Pair<Bid, BigDecimal>(
                this.bidsHash.get(this.sortedUtilKeys.get(this.sortedUtilKeys.size() - 1)).get(0),
                this.sortedUtilKeys.get(this.sortedUtilKeys.size() - 1));

    }

    public AbstractPolicy(UtilitySpace uSpace, String name) {
        super();
        this.setDomain(uSpace.getDomain());
        this.setName(name);
        this.setPartyId(new PartyId("Simulated_" + name));
        this.setUtilitySpace(uSpace);
        this.setBidspace(new AllBidsList(this.getDomain()));
        this.setRandom(new Random());

        BigDecimal currUtility = BigDecimal.ZERO;
        LinkedList<Bid> currBidsWithUtil = new LinkedList<>();
        for (Bid bid : this.getBidspace()) {
            currUtility = this.getUtilitySpace().getUtility(bid);
            if (this.bidsHash.containsKey(currUtility)) {
                currBidsWithUtil = this.bidsHash.get(currUtility);
                currBidsWithUtil.add(bid);
                this.bidsHash.put(currUtility, currBidsWithUtil);
                continue;
            }
            this.sortedUtilKeys.add(currUtility);
            currBidsWithUtil = (LinkedList<Bid>) Stream.of(bid).collect(Collectors.toList());
            this.bidsHash.put(currUtility, currBidsWithUtil);
        }
        this.sortedUtilKeys.sort(new Comparator<BigDecimal>() {
            @Override
            public int compare(BigDecimal o1, BigDecimal o2) {
                return o1.compareTo(o2);
            }
        });

        this.minBidWithUtil = new Pair<Bid, BigDecimal>(this.bidsHash.get(this.sortedUtilKeys.get(0)).get(0),
                this.sortedUtilKeys.get(0));
        this.maxBidWithUtil = new Pair<Bid, BigDecimal>(
                this.bidsHash.get(this.sortedUtilKeys.get(this.sortedUtilKeys.size() - 1)).get(0),
                this.sortedUtilKeys.get(this.sortedUtilKeys.size() - 1));

    }

    public BigDecimal bidBinarySearch(BigDecimal value, ArrayList<BigDecimal> bidSearchSpace, int lowerBound,
            int upperBound, Pair<BigDecimal, BigDecimal> closestValue) {
        if (upperBound <= lowerBound) {
            return bidSearchSpace.get(lowerBound).subtract(value).abs().compareTo(closestValue.getValue()) == -1
                    ? bidSearchSpace.get(lowerBound)
                    : closestValue.getKey();
        }

        int midPoint = (lowerBound + upperBound) / 2;
        BigDecimal middleUtil = bidSearchSpace.get(midPoint);
        closestValue = middleUtil.subtract(value).abs().compareTo(closestValue.getValue()) == -1
                ? new Pair<BigDecimal, BigDecimal>(middleUtil, middleUtil.subtract(value).abs())
                : closestValue;

        if (value.compareTo(middleUtil) == 0) {
            return middleUtil;
        }

        if (value.compareTo(middleUtil) < 0) {
            return bidBinarySearch(value, bidSearchSpace, lowerBound, midPoint - 1, closestValue);
        }

        if (value.compareTo(middleUtil) > 0) {
            return bidBinarySearch(value, bidSearchSpace, midPoint + 1, upperBound, closestValue);
        }

        // should not be reached
        return BigDecimal.ZERO.subtract(BigDecimal.ONE);

    }

    public Bid getBidWithUtility(BigDecimal util) {
        Pair<BigDecimal, BigDecimal> closestValue = new Pair<BigDecimal, BigDecimal>(BigDecimal.ONE, BigDecimal.TEN);
        BigDecimal closestUtil = this.bidBinarySearch(util,
                this.sortedUtilKeys, 0, this.sortedUtilKeys.size() - 1, closestValue);
        // System.out.println("CLOSEST UTILITY");
        // System.out.println(closestUtil);
        return this.bidsHash.get(closestUtil).get(0);
        
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
        List<BigDecimal> allInts = this.getRandom().ints(issues.size(), 0, 100).boxed().map(String::valueOf)
                .map(BigDecimal::new).collect(Collectors.toList());
        BigDecimal sumOfInts = allInts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> issueWeights = IntStream.range(0, issues.size()).boxed().collect(
                Collectors.toMap(issues::get, index -> allInts.get(index).divide(sumOfInts, 5, RoundingMode.HALF_UP)));
        // To make everything add to 1
        BigDecimal remainder = BigDecimal.ONE
                .subtract(issueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        String firstKey = (String) issueWeights.keySet().toArray()[0];
        issueWeights.computeIfPresent(firstKey, (key, value) -> value.add(remainder));
        // System.out.println(issueWeights);

        HashMap<String, ValueSetUtilities> issueValueWeights = new HashMap<String, ValueSetUtilities>();
        for (String issueString : issues) {

            DiscreteValueSet values = (DiscreteValueSet) domain.getValues(issueString);
            List<BigDecimal> randLongs = random.longs(values.size().longValue(), 1, 100).boxed().map(String::valueOf)
                    .map(BigDecimal::new).collect(Collectors.toList());
            // BigDecimal sumOfValueLongs = randLongs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<DiscreteValue, BigDecimal> valueWeights = LongStream.range(0, values.size().longValue()).boxed()
                    .collect(Collectors.toMap(values::get,
                            index -> randLongs.get(index.intValue()).divide(new BigDecimal("100.0"), 5, RoundingMode.HALF_UP)));
            
            // !! values don't need to add up to 1; if they do -> bidWithUtils returns intervals like [0.01, 0.2]
            // Map<DiscreteValue, BigDecimal> valueWeights = LongStream.range(0, values.size().longValue()).boxed()
            //         .collect(Collectors.toMap(values::get,
            //                 index -> randLongs.get(index.intValue()).divide(sumOfValueLongs, 5, RoundingMode.HALF_UP)));
            // // To make everything add to 1
            // BigDecimal valueRemainder = BigDecimal.ONE
            //         .subtract(valueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            // DiscreteValue firstValueKey = (DiscreteValue) valueWeights.keySet().toArray()[valueWeights.keySet().size()
            //         - 1];
            // valueWeights.computeIfPresent(firstValueKey, (key, value) -> value.add(valueRemainder));
            
            issueValueWeights.put(issueString, new DiscreteValueSetUtilities(valueWeights));
        }
        // System.out.println(issueValueWeights);

        return new AbstractMap.SimpleEntry<HashMap<String, ValueSetUtilities>, Map<String, BigDecimal>>(
                issueValueWeights, issueWeights);
    }

    @Override
    public String toString() {
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
        return this.random;
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

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, lastOwnBid, state);
    }

    public void setRandom(Random random) {
        this.random = random;
    }

}
