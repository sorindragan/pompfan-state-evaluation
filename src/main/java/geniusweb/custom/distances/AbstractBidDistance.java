package geniusweb.custom.distances;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import geniusweb.custom.helper.BidVector;
import geniusweb.custom.helper.IVPair;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.ValueSet;
import geniusweb.profile.utilityspace.UtilitySpace;

public abstract class AbstractBidDistance {
    private Domain domain;
    private UtilitySpace utilitySpace;
    private UtilitySpace issues;
    private Map<String, ValueSet> issueValues;
    
    public AbstractBidDistance(UtilitySpace utilitySpace) {
        this.domain = utilitySpace.getDomain();
        this.utilitySpace = utilitySpace;
        this.issues = utilitySpace;
        // this.issueValues = IVPair.getIssueValueSets(this.domain);
    }
    public Domain getDomain() {
        return domain;
    }
    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Double getUtility(Bid b1){
        return this.getUtilitySpace().getUtility(b1).doubleValue();
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }
    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }
    public UtilitySpace getIssues() {
        return issues;
    }
    public void setIssues(UtilitySpace issues) {
        this.issues = issues;
    }
    public Map<String, ValueSet> getIssueValues() {
        return issueValues;
    }
    public void setIssueValues(Map<String, ValueSet> issueValues) {
        this.issueValues = issueValues;
    }
    public Bid computeMostSimilar(Bid b1, List<Bid> allBids){
        
        try {
            Collections.min(this.computeDistances(b1, allBids).entrySet(), Map.Entry.comparingByValue()).getKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        Bid key = Collections.min(this.computeDistances(b1, allBids).entrySet(), Map.Entry.comparingByValue()).getKey();
        return key;
    };

    public Map<Bid, Double> computeDistances(Bid b1, List<Bid> b2) {
        return b2.parallelStream().collect(Collectors.toMap(bid -> bid, bid -> this.computeDistance(b1, bid)));
    }
    public abstract Double computeDistance(Bid b1, Bid b2);

}
