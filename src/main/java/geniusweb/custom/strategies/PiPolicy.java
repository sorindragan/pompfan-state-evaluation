package geniusweb.custom.strategies;

import java.util.HashMap;
import java.util.Map;

import geniusweb.actions.Action;
import geniusweb.custom.helper.IVPair;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;

public class PiPolicy extends AbstractPolicy {
    Map<Bid, AbstractState<?>> policies = new HashMap<Bid, AbstractState<?>>();
    public PiPolicy(Domain domain, String name) {
        super(domain, name);
        IVPair.getVectorContainer(domain);
    }

    @Override
    public Action chooseAction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
