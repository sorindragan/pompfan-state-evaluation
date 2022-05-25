package geniusweb.pompfan.particles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.AllBidsList;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.helper.ExtendedUtilSpace;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.profile.utilityspace.UtilitySpace;
import javafx.collections.transformation.SortedList;
import tudelft.utilities.immutablelist.ImmutableList;

public class OwnUtilityTFTOpponentPolicy extends AbstractPolicy {

    @JsonIgnore
    private Bid myLastbid = null;
    private Bid maxBid;
    private final boolean DEBUG = false;
    private TreeMap<BigDecimal, Bid> searchTree = new TreeMap<>();
    private HashSet<Double> noAssociatedBidsSet = new HashSet<>();

    @JsonCreator
    public OwnUtilityTFTOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace uSpace, 
            @JsonProperty("name") String name) {
        super(uSpace, name);
        this.extendedspace = new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace());
        this.maxBid = this.extendedspace.getBidutils().getExtremeBid(true);
    }
    
    public OwnUtilityTFTOpponentPolicy(Domain domain) {
        super(domain, "OwnUtilTFT");
        this.extendedspace = new ExtendedUtilSpace((LinearAdditiveUtilitySpace) this.getUtilitySpace());
        this.maxBid = this.extendedspace.getBidutils().getExtremeBid(true);
    }
    
    // the ractive agents should have this method implemented
    // used in the particle filter
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        
        ActionWithBid action;

        myLastbid  = lastOwnBid;
        if (second2lastReceivedBid == null) {
            // not enough information known
            Bid bid = this.maxBidWithUtil.getKey();
            action = new Offer(this.getPartyId(), bid);
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = second2lastReceivedBid;
        Bid justLastOpponentBid = lastReceivedBid;
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal utilityGoal = isConcession
                ? this.getUtilitySpace().getUtility(myLastbid).subtract(difference.abs())
                        .max(this.maxBidWithUtil.getValue().divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(this.maxBidWithUtil.getValue());

        // long t = System.nanoTime();
        Bid selectedBid = computeNextBid(utilityGoal);
        if (selectedBid == null) {
            return new Accept(this.getPartyId(), justLastOpponentBid);
        }
        // if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t) > 2000) {
        //     System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        //     System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t));
        //     System.out.println(utilityGoal.doubleValue());
        //     System.out.println(this.bidutils.getRange());
        //     System.out.println("WTF");
        // }

        double time = state.getTime();

        if (DEBUG) {
            System.out.println("============================");
            System.out.println("Used for Belief Update");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
        }

        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                && time > 0.8
                        ? new Accept(this.getPartyId(), justLastOpponentBid)
                        : new Offer(this.getPartyId(), selectedBid);
    }

    // used in constructing the tree
    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
            ActionWithBid action;
        ArrayList<Action> simulatedHistory = ((HistoryState) state).getHistory();

        if (simulatedHistory.size() < 3) {
            // not enough information known
            Bid bid = this.maxBidWithUtil.getKey();
            myLastbid = bid;
            action = new Offer(this.getPartyId(), bid);
            return action;
        }

        BigDecimal difference = BigDecimal.ZERO;
        Bid lastLastOpponentBid = ((ActionWithBid) simulatedHistory.get(simulatedHistory.size()-3)).getBid();
        Bid justLastOpponentBid = ((ActionWithBid) simulatedHistory.get(simulatedHistory.size()-1)).getBid();
        difference = this.getUtilitySpace().getUtility(lastLastOpponentBid)
                .subtract(this.getUtilitySpace().getUtility(justLastOpponentBid));
        boolean isConcession = difference.doubleValue() > 0 ? true : false;

        BigDecimal utilityGoal = isConcession
                ? this.getUtilitySpace().getUtility(myLastbid).subtract(difference.abs())
                        .max(this.maxBidWithUtil.getValue().divide(new BigDecimal("2.0")))
                : this.getUtilitySpace().getUtility(myLastbid).add(difference.abs())
                        .min(this.maxBidWithUtil.getValue());

        Bid selectedBid = computeNextBid(utilityGoal);
        if (selectedBid == null) {
            return new Accept(this.getPartyId(), justLastOpponentBid);
        }        
        double time = state.getTime();

        if (DEBUG) {
            System.out.println("============================");
            System.out.println("Used in particles");
            System.out.println(time);
            System.out.println(selectedBid);
            System.out.println("TFT-Last-Utility: " + this.getUtilitySpace().getUtility(myLastbid));
            System.out.println("TFT-Difference: " + difference);
            System.out.println("TFT-Utility-Goal: " + utilityGoal);
        }
        myLastbid = selectedBid;
        return this.getUtilitySpace().isPreferredOrEqual(justLastOpponentBid, selectedBid)
                && time > 0.8
                        ? new Accept(this.getPartyId(), justLastOpponentBid)
                        : new Offer(this.getPartyId(), selectedBid);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        return this.chooseAction(state);
    }

    @Override
    public Action chooseAction(AbstractState<?> state) {
        // this should not happen
        return super.chooseAction(state);
    }

    private Bid computeNextBid(BigDecimal utilityGoal) {

        if (this.getNoAssociatedBidsSet().contains(utilityGoal.doubleValue())) {
            this.extendedspace.setTolerance(this.extendedspace.getTolerance().multiply(BigDecimal.valueOf(2.0)));
            System.out.println("TOLERANCE: " + this.extendedspace.getTolerance());
        }


        if (utilityGoal.doubleValue() < this.extendedspace.getBidutils().getRange().getMin().doubleValue()) {
            utilityGoal = this.extendedspace.getBidutils().getRange().getMin();
        }

        if (utilityGoal.doubleValue() > this.extendedspace.getBidutils().getRange().getMax().doubleValue()) {
            utilityGoal = this.extendedspace.getBidutils().getRange().getMax();
        }

        ImmutableList<Bid> options = this.extendedspace.getBids(utilityGoal);
        
        try {
            return this.parseOptions(options, utilityGoal);      
        } catch (Exception e) {
            // System.out.println("WARNING: A profile was genereated weirdly and resulted in option size:"
            //         + options.size().intValue());
            // System.out.println("The seearch tree has:" + this.getSearchTree().size());
            // System.out.println("The utility goals was:" + utilityGoal);
            this.getNoAssociatedBidsSet().add(utilityGoal.doubleValue());
            System.out.println("NO BIDS FOR");
            System.out.println(this.getNoAssociatedBidsSet());
            return null;
        }
    }

    private Bid parseOptions(ImmutableList<Bid> options, BigDecimal targetUtil) {
        // System.out.println(options.size());
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

        // System.out.println("T:" + targetUtil.setScale(6, RoundingMode.DOWN) + " C:" +
        // this.getUtilitySpace().getUtility(chosenBid) + " " + chosenBid);
        return chosenBid;
    }

    public TreeMap<BigDecimal, Bid> getSearchTree() {
        return searchTree;
    }

    public void setSearchTree(TreeMap<BigDecimal, Bid> searchTree) {
        this.searchTree = searchTree;
    }

    public HashSet<Double> getNoAssociatedBidsSet() {
        return noAssociatedBidsSet;
    }

    public void setNoAssociatedBidsSet(HashSet<Double> noAssociatedBidsSet) {
        this.noAssociatedBidsSet = noAssociatedBidsSet;
    }
}
