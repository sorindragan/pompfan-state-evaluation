package geniusweb.pompfan.opponents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class BoulwareOpponentPolicy extends TimeDependentOpponentPolicy {

    private static final String BOULWARE = "Boulware";

    @JsonCreator
    public BoulwareOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace,
            @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, name, e);
    }

    // public BoulwareOpponentPolicy(UtilitySpace utilitySpace) {
    //     super(utilitySpace, BOULWARE, 0.2);
    // }

    public BoulwareOpponentPolicy(Domain domain) {
        super(domain, BOULWARE);
    }

    @Override
    public double getE() {
        return 0.2;
    }

}
