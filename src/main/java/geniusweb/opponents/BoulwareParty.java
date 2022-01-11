package geniusweb.opponents;

public class BoulwareParty extends TimeDependentParty {

    private static final String BOULWARE = "Boulware";
    private double e = 0.2;

    public BoulwareParty() {
        super();
    }


    @Override
    public double getE() {
        return 0.2;
    }

}
