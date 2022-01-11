package geniusweb.opponents;

public class ConcederParty extends TimeDependentParty {
    private static final String CONCEDER = "Conceder";
    private double e = 2.0;

    public ConcederParty() {
        super();
    }

    @Override
    public double getE() {
        return 2.0;
    }
    
}
