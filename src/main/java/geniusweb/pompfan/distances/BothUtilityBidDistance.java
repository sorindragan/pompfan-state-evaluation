package geniusweb.pompfan.distances;

import java.util.HashMap;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;
import geniusweb.profile.utilityspace.UtilitySpace;

public class BothUtilityBidDistance extends AbstractBidDistance {
    // same as UtilityBidDistance, but a different class to distinguish the usage
    // of the both the acting agent utility function togheter with the sampled opponent utility function
    @JsonCreator
    public BothUtilityBidDistance(@JsonProperty("utilitySpace") UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {

        Bid tmpb1 = b1;
        Bid tmpb2 = b2;
        // Map<String, Value> t = new HashMap<>(b1.getIssueValues());
        // String toRemove = t.keySet().parallelStream().findFirst().get();
        // t.remove(toRemove);
        // tmpb1 = new Bid(t);
        // Bid ftmpb1 = new Bid(t);
        for (String issue : this.getDomain().getIssues()) {
            Value value = null;

            if (!tmpb2.containsIssue(issue)) {
                value = tmpb1.getValue(issue);
                tmpb2 = completeBid(tmpb2, issue, value);
            }
            if (!tmpb1.containsIssue(issue)) {
                value = tmpb2.getValue(issue);
                tmpb1 = completeBid(tmpb1, issue, value);
            }
            if (!tmpb1.containsIssue(issue) && !tmpb2.containsIssue(issue)) {
                ValueSet iValues = this.getDomain().getValues(issue);
                value = iValues.get(new Random().nextInt(iValues.size().intValue()));
                tmpb1 = completeBid(tmpb1, issue, value);
                tmpb2 = completeBid(tmpb2, issue, value);
            }
            // check for uncomplete bids
            // if (!ftmpb1.containsIssue(issue) || !tmpb2.containsIssue(issue)) {
            //     System.out.println("-----");
            //     System.out.println(b1);
            //     System.out.println("-----");
            //     System.out.println(ftmpb1);
            //     System.out.println("-----");
            //     System.out.println(tmpb1);
            //     System.out.println("-----");
            //     System.out.println(b2);
            //     System.out.println("-----");
            //     System.out.println(tmpb2);
            //     System.exit(0);
            // }
            // System.exit(0);

        }

        return Math.abs(this.getUtility(tmpb1) - this.getUtility(tmpb2));
    }

    private Bid completeBid(Bid b1, String issue, Value value) {
        HashMap<String, Value> diffBid = null;
        diffBid = new HashMap<String, Value>(b1.getIssueValues());
        diffBid.putIfAbsent(issue, value);
        b1 = new Bid(diffBid);
        return b1;
    }

}
