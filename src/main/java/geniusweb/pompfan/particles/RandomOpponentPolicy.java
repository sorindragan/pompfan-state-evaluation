package geniusweb.pompfan.particles;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class RandomOpponentPolicy extends AbstractPolicy {

    private static final float stubborness = 0.95f;

    public RandomOpponentPolicy(UtilitySpace uSpace, String name) {
        super(uSpace, name);
    }

    public RandomOpponentPolicy(Domain domain) {
        super(domain, "Random");
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, Bid second2lastReceivedBid,
            AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, lastOwnBid, state);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        Action action;
        Offer offer = (Offer) super.chooseAction();
        if (isDecent(offer.getBid())) {
            action = new Accept(this.getPartyId(), lastReceivedBid);
            // System.out.println(this.getName() + " generated ACCEPT");
        } else {
            action = offer;
        }
        // action = isDecent(offer.getBid()) ? new Accept(this.getPartyId(), lastReceivedBid) : offer;

        return action;
    }

    private boolean isDecent(Bid bid) {
        if (bid == null)
            return false;

        float sample = Math.abs(this.getRandom().nextFloat());
        return sample > stubborness ? true : false;
    }

}
