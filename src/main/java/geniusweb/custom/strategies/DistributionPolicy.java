package geniusweb.custom.strategies;

import java.math.BigInteger;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;

public class DistributionPolicy extends AbstractPolicy {
    

    private static final float stubborness = 0.98f;

    public DistributionPolicy(Domain domain) {
        super(domain, "Random");

    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState state) {
        Action action;
        // long i = this.getRandom();
        // Bid bid = this.getBidspace().get(BigInteger.valueOf(i));
        // action = isGood(bid) ? new Offer(this.getPartyId(), bid) : new Accept(this.getPartyId(), lastAgentBid);
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

        return this.getRandom().nextFloat() > stubborness ? true : false;
        
    }

    @Override
    public Action chooseAction(Bid lastAgentBid) {
        // TODO Auto-generated method stub
        return null;
    }


}
