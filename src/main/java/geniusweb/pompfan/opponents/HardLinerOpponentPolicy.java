package geniusweb.pompfan.opponents;

import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class HardLinerOpponentPolicy extends TimeDependentOpponentPolicy {

    private final static String HARDLINER = "HardLiner";

    public HardLinerOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, HARDLINER, e);
    }

    public HardLinerOpponentPolicy(Domain domain) {
        super(domain, HARDLINER);
    }

    @Override
    public double getE() {
        return 0.0;
    }

}
