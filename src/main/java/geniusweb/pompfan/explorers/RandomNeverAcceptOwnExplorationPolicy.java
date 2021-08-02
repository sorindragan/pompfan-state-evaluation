package geniusweb.pompfan.explorers;

import java.math.BigDecimal;
import java.math.BigInteger;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class RandomNeverAcceptOwnExplorationPolicy extends AbstractOwnExplorationPolicy {

    private static final BigDecimal STUBBORNESS = new BigDecimal(0.0);


    public RandomNeverAcceptOwnExplorationPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }


    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);
    }
}
