package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.exampleparties.timedependentparty.ExtendedUtilSpace;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = BoulwareOpponentPolicy.class), @Type(value = ConcederOpponentPolicy.class),
        @Type(value = HardLinerOpponentPolicy.class), @Type(value = LinearOpponentPolicy.class) })
public class TimeDependentOpponentPolicy extends AbstractPolicy {

    private ExtendedUtilSpace extendedspace;
    private double e = 1.2;

    public TimeDependentOpponentPolicy(Domain domain) {
        super(domain, "TimeDependent");
        this.extendedspace = new ExtendedUtilSpace((LinearAdditive) this.getUtilitySpace());
    }
    
    @JsonCreator
    public TimeDependentOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, name);
        this.e = e;
        this.extendedspace = new ExtendedUtilSpace((LinearAdditive) this.getUtilitySpace());
    }


    public TimeDependentOpponentPolicy(Domain domain, String name) {
        super(domain, name);
        this.extendedspace = new ExtendedUtilSpace((LinearAdditive) this.getUtilitySpace());
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.myTurn(lastReceivedBid, state);
    }
    
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, lastOwnBid, state);
    }

    private Action myTurn(Bid lastReceivedBid, AbstractState<?> state) {
        Bid bid = makeBid(state.getTime());
        PartyId me = this.getPartyId();
        UtilitySpace utilspace = this.getUtilitySpace();
        Action myAction;
        if (bid == null || (lastReceivedBid != null
                && utilspace.getUtility(lastReceivedBid)
                        .compareTo(utilspace.getUtility(bid)) >= 0)) {
            // if bid==null we failed to suggest next bid.
            myAction = new Accept(me, lastReceivedBid);
        } else {
            myAction = new Offer(me, bid);
        }
        return myAction;
    }

    /**
     * @return next possible bid with current target utility, or null if no such
     *         bid.
     */
    private Bid makeBid(Double currTime) {

        BigDecimal utilityGoal = getUtilityGoal(currTime, this.e, this.extendedspace.getMin(), this.extendedspace.getMax());
        ImmutableList<Bid> options = this.extendedspace.getBids(utilityGoal);
        if (options.size().compareTo(BigInteger.ONE) == -1) {
            // this should not happen!
            options = extendedspace.getBids(this.extendedspace.getMax());
        }
        return options.get(options.size().intValue()-1);


    }

    /**
     * 
     * @param t       the time in [0,1] where 0 means start of nego and 1 the end of
     *                nego (absolute time/round limit)
     * @param e       the e value that determinses how fast the party makes
     *                concessions with time. Typically around 1. 0 means no
     *                concession, 1 linear concession, &gt;1 faster than linear
     *                concession.
     * @param minUtil the minimum utility possible in our profile
     * @param maxUtil the maximum utility possible in our profile
     * @return the utility goal for this time and e value
     */
    protected BigDecimal getUtilityGoal(double t, double e, BigDecimal minUtil, BigDecimal maxUtil) {

        double ft = 0.0;
        if (e != 0) ft = Math.pow(t, 1 / e);
        // we subtract epsilon to correct possibly small round-up errors
        return new BigDecimal(minUtil.doubleValue()
                        + (maxUtil.doubleValue() - minUtil.doubleValue()) * (1 - ft))
                                        .min(maxUtil).max(minUtil);
    }

    /**
     * @return the E value that controls the party's behaviour. Depending on the
     *         value of e, extreme sets show clearly different patterns of behaviour
     *         [1]:
     * 
     *         1. Boulware: For this strategy e &lt; 1 and the initial offer is
     *         maintained till time is almost exhausted, when the agent concedes up
     *         to its reservation value.
     * 
     *         2. Conceder: For this strategy e &gt; 1 and the agent goes to its
     *         reservation value very quickly.
     * 
     *         3. When e = 1, the price is increased linearly.
     * 
     *         4. When e = 0, the agent plays hardball.
     */
    public double getE() {
        return e;
    }
}
