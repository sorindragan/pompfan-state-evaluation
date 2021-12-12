package geniusweb.pompfan.distances;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class SDiceBidDistance extends AbstractBidDistance {

    @JsonCreator
    public SDiceBidDistance(@JsonProperty("utilitySpace") UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        Set<String> issues = this.getUtilitySpace().getDomain().getIssues();
        int cntSame = 0;
        int cntTotal = 0;

        for (String issue : issues) {
            if (b1.containsIssue(issue) && b2.containsIssue(issue)) {
                cntTotal++;
                cntSame = b1.getValue(issue).equals(b2.getValue(issue)) ? ++cntSame : cntSame;
            }
        }

        Double dist = (2*cntSame) / (double) (2*cntTotal);

        return 1 - dist;
    }

}
