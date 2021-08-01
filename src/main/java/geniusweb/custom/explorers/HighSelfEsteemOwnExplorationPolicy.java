package geniusweb.custom.explorers;

import java.util.Random;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.bidspace.Interval;
import geniusweb.boa.biddingstrategy.ExtendedUtilSpace;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;
import tudelft.utilities.immutablelist.ImmutableList;

// Class that never explores bids below what the opponent is already giving us
public class HighSelfEsteemOwnExplorationPolicy extends AbstractOwnExplorationPolicy {

    private BidsWithUtility bidutils;

    // private ExtendedUtilSpace extendedspace;
    public HighSelfEsteemOwnExplorationPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
        this.bidutils = new BidsWithUtility((LinearAdditive) this.getUtilitySpace());
        // this.extendedspace = new ExtendedUtilSpace((LinearAdditive) utilitySpace);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        Action action;
        Bid bid;
        // progressively raise the percentages of accepts from 0 to 10 pcent
        double acceptProbability = state.getTime() * 10;
        
        
        if (lastReceivedBid == null) {
            bid = this.getAllBids().getExtremeBid(true);
        } else {
            ImmutableList<Bid> options = this.getBidutils()
                    .getBids(new Interval(this.getUtilitySpace().getUtility(lastReceivedBid),
                            this.getUtilitySpace().getUtility(this.getBidutils().getExtremeBid(true))));
            
            bid = options.get(new Random().nextInt(options.size().intValue()));
        }
        action = new Offer(this.getPartyId(), bid);
        
        // pcent of the time we do accepts
        long i = this.getRandom().nextInt(100);
        if (i > (100 - acceptProbability)) {
            action = new Accept(this.getPartyId(), lastReceivedBid);
        }

        return action;
    }

    @Override
    protected void init() {assert true;}

    public BidsWithUtility getBidutils() {
        return bidutils;
    }

    public void setBidutils(BidsWithUtility bidutils) {
        this.bidutils = bidutils;
    }

}
