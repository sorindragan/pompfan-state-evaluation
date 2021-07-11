package geniusweb.custom.opponents;

import java.math.BigInteger;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;

public class RandomOpponentPolicy extends AbstractPolicy {

    private static final float stubborness = 0.95f;

    public RandomOpponentPolicy(Domain domain) {
        super(domain, "Random");

    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        Action action;
        Offer offer = (Offer) this.chooseAction();
        action = isGood(offer.getBid()) ? new Accept(this.getPartyId(), lastAgentBid) : offer;
        System.out.println(action);
        return action;
    }

    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;

        float sample = Math.abs(this.getRandom().nextFloat());
        return sample > stubborness ? true : false;
    }

}
