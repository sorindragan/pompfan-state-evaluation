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
    public Action chooseAction(Bid lastAgentBid) {
        Action action;
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        action = isGood(bid) ? new Accept(this.getPartyId(), lastAgentBid) : new Offer(this.getPartyId(), bid);
        return  action;
    }
    
    @Override
    public Action chooseAction() {
        long i = this.getRandom().nextInt(this.getBidspace().size().intValue());
        Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        return new Offer(this.getPartyId(), bid) ;
    }

    private boolean isGood(Bid bid) {
        if (bid == null)
            return false;

        float sample = Math.abs(this.getRandom().nextFloat());
        System.out.println("SAMPLE");
        System.out.println(sample);
        return sample > stubborness ? true : false;
        
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        return this.chooseAction();
    }


}
