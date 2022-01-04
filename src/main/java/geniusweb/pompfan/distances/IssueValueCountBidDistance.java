package geniusweb.pompfan.distances;

import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class IssueValueCountBidDistance extends AbstractBidDistance {

    public IssueValueCountBidDistance(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        Double differentValues = 0.0;
        Integer noOfValueas = 0;
        Bid tmpb1 = b1;
        Bid tmpb2 = b2;
        for (String issue : this.getDomain().getIssues()) {
            // System.out.println(issue);
            if (tmpb1.containsIssue(issue) && tmpb2.containsIssue(issue)) {
                differentValues = tmpb1.getValue(issue).equals(tmpb2.getValue(issue)) ? differentValues : ++differentValues;
            }
            noOfValueas++;
        }
        Double distance = differentValues / noOfValueas.doubleValue();
        return distance;
    }

}
