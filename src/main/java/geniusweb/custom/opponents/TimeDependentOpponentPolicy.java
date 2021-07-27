package geniusweb.custom.opponents;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Random;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.custom.state.AbstractState;
import geniusweb.exampleparties.timedependentparty.ExtendedUtilSpace;
import geniusweb.exampleparties.timedependentparty.TimeDependentParty;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.profileconnection.ProfileInterface;
import geniusweb.progress.Progress;
import tudelft.utilities.immutablelist.ImmutableList;

public class TimeDependentOpponentPolicy extends AbstractPolicy {

    // private ProfileInterface profileint = null;
    private LinearAdditive utilspace = null; // last received space
    // private PartyId me;
    // private Progress progress;
    private ExtendedUtilSpace extendedspace;
    private double e = 1.2;
    // private Settings settings;

    public TimeDependentOpponentPolicy(Domain domain) {
        super(domain, "TimeDependent");
        this.utilspace = (LinearAdditive) this.getUtilitySpace();
        this.extendedspace = new ExtendedUtilSpace(this.utilspace);
        // this.maxBid = this.extendedspace.
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        // if(lastReceivedBid==null){
        // ImmutableList<Bid> bids = this.extendedspace.getBids(BigDecimal.ONE);
        // Bid selectedBid = bids.get(this.getRandom().nextInt(bids.size().intValue()));
        // return new Offer(this.getPartyId(), selectedBid);
        // }
        return this.myTurn(lastReceivedBid, state);
    }

    private Action myTurn(Bid lastReceivedBid, AbstractState<?> state) {
        Bid bid = makeBid(state.getRound());

        if (bid == null) {
            // if bid==null we failed to suggest next bid.
            return new Accept(this.getPartyId(), lastReceivedBid);
        }
        Action myAction;
        boolean isLastReceivedIsBiggerThanOwnOffer = false;
        if (lastReceivedBid != null) {
            BigDecimal lastReceivedUtility = this.utilspace.getUtility(lastReceivedBid);
            isLastReceivedIsBiggerThanOwnOffer = lastReceivedUtility.compareTo(this.utilspace.getUtility(bid)) >= 0;
        }
        if (isLastReceivedIsBiggerThanOwnOffer) {
            myAction = new Accept(this.getPartyId(), lastReceivedBid);
        } else {
            myAction = new Offer(this.getPartyId(), bid);
        }
        return myAction;
    }

    /**
     * @return next possible bid with current target utility, or null if no such
     *         bid.
     */
    private Bid makeBid(Double currTime) {
        // double time = progress.get(System.currentTimeMillis());

        BigDecimal utilityGoal = getUtilityGoal(currTime, getE(), extendedspace.getMin(), extendedspace.getMax());
        ImmutableList<Bid> options = extendedspace.getBids(utilityGoal);
        if (options.size().compareTo(BigInteger.ONE) == -1) {
            // if we can't find good bid, get max util bid and if no max bid take min bid as tolerance....
            ImmutableList<Bid> alternativeOptions = extendedspace.getBids(extendedspace.getMax());
            alternativeOptions = alternativeOptions.size().compareTo(BigInteger.ONE) < 1
                    ? extendedspace.getBids(extendedspace.getMin())
                    : alternativeOptions;
            return alternativeOptions.get(0l);
        }
        // pick a random one.
        // if(options.size().intValue() < 1){
        // System.out.println(options.size().intValue());;
        // }
        return options.get(new Random().nextInt(options.size().intValue()));

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

        BigDecimal ft1 = BigDecimal.ONE;
        if (e != 0)
            ft1 = BigDecimal.valueOf(1 - Math.pow(t, 1 / e)).setScale(6, RoundingMode.HALF_UP);
        return minUtil.add((maxUtil.subtract(minUtil).multiply(ft1))).min(maxUtil).max(minUtil);
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
