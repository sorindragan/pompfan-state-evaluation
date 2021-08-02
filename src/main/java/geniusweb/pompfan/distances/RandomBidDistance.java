package geniusweb.pompfan.distances;

import java.util.Random;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class RandomBidDistance extends AbstractBidDistance {
    private final Random random = new Random();

    public RandomBidDistance(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        return this.random.nextDouble();
    }
    
}
