package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import geniusweb.actions.Action;
import geniusweb.bidspace.AllBidsList;
import geniusweb.custom.state.AbstractState;
import geniusweb.exampleparties.boulware.Boulware;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import tudelft.utilities.immutablelist.ImmutableList;

public class BoulwareOpponentPolicy extends TimeDependentOpponentPolicy {

    private static final String BOULWARE = "Boulware";

    public BoulwareOpponentPolicy(Domain domain) {
        super(domain);
        this.setName(BOULWARE);
    }

    @Override
    public double getE() {
        return 0.2;
    }

}
