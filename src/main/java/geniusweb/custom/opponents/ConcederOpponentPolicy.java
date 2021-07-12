package geniusweb.custom.opponents;

import geniusweb.issuevalue.Domain;

public class ConcederOpponentPolicy extends TimeDependentOpponentPolicy{

    /**
     *
     */
    private static final String CONCEDER = "Conceder";

    public ConcederOpponentPolicy(Domain domain) {
        super(domain);
        this.setName(CONCEDER);
    }
    
    @Override
    public double getE() {
        return 2.0;
    }
}
