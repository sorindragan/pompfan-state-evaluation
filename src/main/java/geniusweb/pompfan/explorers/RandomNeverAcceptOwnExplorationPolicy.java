package geniusweb.pompfan.explorers;

import java.math.BigDecimal;

import geniusweb.actions.PartyId;
import geniusweb.profile.utilityspace.UtilitySpace;

public class RandomNeverAcceptOwnExplorationPolicy extends AbstractOwnExplorationPolicy {

    private static final BigDecimal STUBBORNESS = BigDecimal.ONE;


    public RandomNeverAcceptOwnExplorationPolicy(UtilitySpace utilitySpace, PartyId id) {
        super(utilitySpace, id);
    }

    @Override
    protected void init() {
        this.setSTUBBORNESS(STUBBORNESS);
    }
}
