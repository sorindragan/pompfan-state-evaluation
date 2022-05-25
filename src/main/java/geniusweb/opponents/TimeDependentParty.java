package geniusweb.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.inform.Agreements;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.LinearAdditiveUtilitySpace;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

public class TimeDependentParty extends AbstractOpponent {

    private ExtendedUtilSpace extendedspace;
    private double e = 1.2;
    private final boolean DEBUG = true;

    public TimeDependentParty() {
        super();
    }

    // public TimeDependentParty(Reporter reporter) {
    //     super(reporter); // for debugging
    // }

    public double getE() {
        return e;
    }

    protected ActionWithBid myTurn(Object param) {
        // this.extendedspace = new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace());
        Bid bid = makeBid();
        PartyId me = this.getMe();
        UtilitySpace utilspace = this.getUtilitySpace();
        List<ActionWithBid> oppHistory = this.getOpponentHistory();
        Bid lastReceivedBid;
        if (oppHistory.size() < 1) {
            lastReceivedBid = null;
        } else {
            lastReceivedBid = oppHistory.get(oppHistory.size()-1).getBid();
        }
        Action myAction;
        if (bid == null || (lastReceivedBid != null
                && utilspace.getUtility(lastReceivedBid)
                        .compareTo(utilspace.getUtility(bid)) >= 0)) {
            // if bid==null we failed to suggest next bid.
            myAction = new Accept(me, lastReceivedBid);
        } else {
            myAction = new Offer(me, bid);
        }
        return (ActionWithBid) myAction;

    }

    /**
     * @return next possible bid with current target utility, or null if no such
     *         bid.
     */
    private Bid makeBid() {
        double time = this.getProgress().get(System.currentTimeMillis());
        BigDecimal utilityGoal = utilityGoal(time, getE());
        ImmutableList<Bid> options = this.getExtendedspace().getBids(utilityGoal);
        if (options.size() == BigInteger.ZERO) {
            // System.out.println("NADA");
            // if we can't find good bid, get max util bid....
            options = this.getExtendedspace().getBids(this.getExtendedspace().getMax());
        }
        if (DEBUG) {
            // System.out.println("O: " + time);
            // System.out.println("TD-Utility-Goal: " + utilityGoal.doubleValue());
            // System.out.println("TD-Returned-Utility: " + this.getUtilitySpace().getUtility(options.get(options.size().intValue() - 1)));
            // System.out.println(time + "," +  utilityGoal.doubleValue() + "," + this.getUtilitySpace().getUtility(options.get(options.size().intValue() - 1)));
        }
        return this.parseOptions(options, utilityGoal);
        // System.out.println("OR " + time + "T " + options.get(options.size().intValue() - 1) + " - " + this.getUtilitySpace().getUtility(options.get(options.size().intValue() - 1)));
        
        // return options.get(options.size().intValue()-1);

    }

    private Bid parseOptions(ImmutableList<Bid> options, BigDecimal targetUtil) {
        System.out.println(options.size());
        if (options.size().intValue() == 1) {
            return options.get(0);
        }

        if (options.size().intValue() == 0) {
            BigDecimal closestExistentKey = this.getSearchTree().lowerKey(targetUtil);
            closestExistentKey = closestExistentKey == null ? this.getSearchTree().higherKey(targetUtil)
                    : closestExistentKey;
            return this.getSearchTree().get(closestExistentKey);
        }
        Iterator<Bid> optionIterator = options.iterator();

        // options.forEach(this.getUtilitySpace()::getUtility);
        while (optionIterator.hasNext()) {
            Bid currBid = optionIterator.next();
            this.getSearchTree().put(this.getUtilitySpace().getUtility(currBid), currBid);
        }
        BigDecimal closestKey = this.getSearchTree().lowerKey(targetUtil);
        closestKey = closestKey == null ? this.getSearchTree().higherKey(targetUtil) : closestKey;
        Bid chosenBid = this.getSearchTree().get(closestKey);

        System.out.println("T:" + targetUtil.setScale(6, RoundingMode.DOWN) + " C:" +
        this.getUtilitySpace().getUtility(chosenBid) + " " + chosenBid);
        return chosenBid;
    }

    /**
     * 
     * @param t the time in [0,1] where 0 means start of nego and 1 the end of
     *          nego (absolute time/round limit)
     * @param e the e value
     * @return the utility goal for this time and e value
     */
    private BigDecimal utilityGoal(double t, double e) {
        BigDecimal minUtil = this.getExtendedspace().getMin();
        BigDecimal maxUtil = this.getExtendedspace().getMax();
        double ft = 0;
        if (e != 0)
            ft = Math.pow(t, 1 / e);
        // we subtract epsilon to correct possibly small round-up errors
        return new BigDecimal(minUtil.doubleValue()
                + (maxUtil.doubleValue() - minUtil.doubleValue()) * (1 - ft))
                        .min(maxUtil).max(minUtil);
    }

    @Override
    protected void processAgreements(Agreements agreements) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void processNonAgreements(Agreements agreements) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void processOpponentAction(ActionWithBid action) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void terminate() {
        super.terminate();
    }
}