package geniusweb.custom.opponents;

import geniusweb.issuevalue.Domain;

public class HardLinerOpponentPolicy extends TimeDependentOpponentPolicy {

    private final static String HARDLINER = "HardLiner";

    public HardLinerOpponentPolicy(Domain domain) {
        super(domain);
        this.setName(HARDLINER);
    }

    @Override
    public double getE() {
        return 0.0;
    }

}
