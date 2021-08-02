package geniusweb.pompfan.opponents;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Domain;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ConcederOpponentPolicy extends TimeDependentOpponentPolicy{

    /**
     *
     */
    private static final String CONCEDER = "Conceder";

    @JsonCreator
    public ConcederOpponentPolicy(@JsonProperty("utilitySpace") UtilitySpace utilitySpace, @JsonProperty("name") String name, @JsonProperty("e") double e) {
        super(utilitySpace, name, e);
    }

    public ConcederOpponentPolicy(Domain domain) {
        super(domain);
        this.setName(CONCEDER);
    }
    
    @Override
    public double getE() {
        return 2.0;
    }
}
