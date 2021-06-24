package geniusweb.custom.distances;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class UtilityBidDistance extends AbstractBidDistance {

    public UtilityBidDistance(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        return Math.abs(this.getUtility(b1)-this.getUtility(b2));
    }
    
}
