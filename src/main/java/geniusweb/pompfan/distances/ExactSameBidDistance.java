package geniusweb.pompfan.distances;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ExactSameBidDistance extends AbstractBidDistance {

    public ExactSameBidDistance(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        Double isSame = 0.0;
        Bid tmpb1 = b1;
        Bid tmpb2 = b2;
        for (String issue : this.getDomain().getIssues()) {
            if (tmpb1.containsIssue(issue) && tmpb2.containsIssue(issue)) {
                isSame = tmpb1.getValue(issue).equals(tmpb2.getValue(issue)) ? 0.0 : 1.0;
                if (isSame > 0.0) break;
            }
        }

        return isSame;
    }

}
