package geniusweb.pompfan.opponents;

import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class LinearOpponentPolicy extends TimeDependentOpponentPolicy {

    private static final String LINEAR = "Linear";

    public LinearOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, LINEAR, e);
    }

    public LinearOpponentPolicy(Domain domain) {
        super(domain, LINEAR);
    }

    @Override
    public double getE() {
        return 1;
    }

}
