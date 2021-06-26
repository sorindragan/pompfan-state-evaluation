package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.exampleparties.boulware.Boulware;
import geniusweb.issuevalue.Bid;
import tudelft.utilities.immutablelist.ImmutableList;

public class BoulwarePolicy extends Boulware implements CommonOpponentInterface {

    @Override
    public Action chooseAction() {
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid) {
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        // double time = this.progress.get(System.currentTimeMillis());

        // BigDecimal utilityGoal = getUtilityGoal(time, getE(),
        // extendedspace.getMin(), extendedspace.getMax());
        // ImmutableList<Bid> options = extendedspace.getBids(utilityGoal);
        // if (options.size() == BigInteger.ZERO) {
        // // if we can't find good bid, get max util bid....
        // options = extendedspace.getBids(extendedspace.getMax());
        // }
        // // pick a random one.
        // return new Offer(this., options.get(new
        // Random().nextInt(options.size().intValue())));
        return null;
    }

}
