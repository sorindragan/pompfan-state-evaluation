package geniusweb.pompfan.distances;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Value;
import geniusweb.pompfan.helper.IVPair;
import geniusweb.pompfan.helper.IVPair.IVPairMapping;
import geniusweb.profile.utilityspace.UtilitySpace;

public class HammingBidDistance extends AbstractBidDistance {

    @JsonIgnore
    private IVPairMapping mapping;

    @JsonCreator
    public HammingBidDistance(@JsonProperty("utilitySpace") UtilitySpace utilitySpace) {
        super(utilitySpace);
        this.setMapping(IVPair.getIVPairMapping(this.getDomain()));
    }

    public IVPairMapping getMapping() {
        return mapping;
    }

    public void setMapping(IVPairMapping mapping) {
        this.mapping = mapping;
    }

    @Override
    public Double computeDistance(Bid b1, Bid b2) {
        Set<String> issues = this.getIssues();
        Map<IVPair, Integer> bidOne = new HashMap<IVPair, Integer>();
        Map<IVPair, Integer> bidTwo = new HashMap<IVPair, Integer>();
        Integer cntSame = 0;
        Integer cntTotal = this.getMapping().size();

        
        for (IVPair ivp : this.getMapping()) {
            bidOne.put(ivp, 0);
            bidTwo.put(ivp, 0);
        }

        for (String issue : issues) {
            IVPair iv1 = new IVPair(issue, b1.getValue(issue));
            IVPair iv2 = new IVPair(issue, b2.getValue(issue));
            bidOne.put(iv1, 1);
            bidTwo.put(iv2, 1);
        }

        for (IVPair ivp : this.getMapping()) {
            Integer bValue1 = bidOne.get(ivp);
            Integer bValue2 = bidTwo.get(ivp);
            if (bValue2 == bValue1) {
                cntSame++;
            }
        }

        Double dist = cntSame / cntTotal.doubleValue();

        return 1 - dist;
    }

}
