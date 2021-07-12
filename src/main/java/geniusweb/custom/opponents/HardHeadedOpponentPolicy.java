package geniusweb.custom.opponents;

import geniusweb.issuevalue.Domain;

public class HardHeadedOpponentPolicy extends TimeDependentOpponentPolicy {

    private final static String HARDHEADED = "HardHeaded";

    public HardHeadedOpponentPolicy(Domain domain) {
        super(domain);
        this.setName(HARDHEADED);
    }

    @Override
    public double getE() {
        return 0.0;
    }

}
