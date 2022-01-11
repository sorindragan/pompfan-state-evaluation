package geniusweb.opponents;

public class LinearParty extends TimeDependentParty {
    private static final String LINEAR = "Linear";
    private double e = 1.0;

    public LinearParty() {
        super();
    }

    @Override
    public double getE() {
        return 1.0;
    }
    
}
