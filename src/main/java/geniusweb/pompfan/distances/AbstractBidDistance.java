package geniusweb.pompfan.distances;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.ValueSet;
import geniusweb.profile.utilityspace.UtilitySpace;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({ @Type(value = RandomBidDistance.class, name = "RandomBidDistance"),
        @Type(value = UtilityBidDistance.class, name = "UtilityBidDistance"),
        @Type(value = JaccardBidDistance.class, name = "JaccardBidDistance"),})
public abstract class AbstractBidDistance {
    private Domain domain;
    private UtilitySpace utilitySpace;
    private Set<String> issues;
    private Map<String, ValueSet> issueValues;

    @JsonCreator
    public AbstractBidDistance(@JsonProperty("utilitySpace") UtilitySpace utilitySpace) {
        this.domain = utilitySpace.getDomain();
        this.utilitySpace = utilitySpace;
        this.issues = utilitySpace.getDomain().getIssues();

    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Double getUtility(Bid b1) {
        return this.getUtilitySpace().getUtility(b1).doubleValue();
    }

    public UtilitySpace getUtilitySpace() {
        return utilitySpace;
    }

    public void setUtilitySpace(UtilitySpace utilitySpace) {
        this.utilitySpace = utilitySpace;
    }

    public Set<String> getIssues() {
        return issues;
    }

    public void setIssues(Set<String> issues) {
        this.issues = issues;
    }

    public Map<String, ValueSet> getIssueValues() {
        return issueValues;
    }

    public void setIssueValues(Map<String, ValueSet> issueValues) {
        this.issueValues = issueValues;
    }

    public Bid computeMostSimilar(Bid b1, List<Bid> allBids) {

        Bid key = Collections.min(this.computeDistances(b1, allBids).entrySet(), Map.Entry.comparingByValue()).getKey();
        return key;
    };

    public Map<Bid, Double> computeDistances(Bid b1, List<Bid> b2) {
        try {
            return b2.parallelStream().distinct().collect(Collectors.toMap(bid -> bid, bid -> this.computeDistance(b1, bid)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public abstract Double computeDistance(Bid b1, Bid b2);

}
