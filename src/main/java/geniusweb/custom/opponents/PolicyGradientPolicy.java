package geniusweb.custom.opponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.rng.Random;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

import geniusweb.actions.Action;
import geniusweb.custom.helper.IVPair;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.ValueSet;

public class PolicyGradientPolicy extends AbstractPolicy {
    private Map<PolicyState, PolicyActionDistribution> policies;
    private List<String> allIssues;

    public PolicyGradientPolicy(Domain domain, String name) {
        super(domain, name);

        allIssues = IVPair.getIssues(domain);
        policies = new HashMap<>();
    }

    @Override
    public Action chooseAction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action chooseAction(Bid lastAgentBid, AbstractState<?> state) {
        PolicyActionDistribution actionDistribution = this.policies.get(state.getRepresentation()); 
        Map<String, INDArray> allIssueWeights = actionDistribution.issueSetsWeights;
        for (String issue : this.allIssues) {
            INDArray weights = actionDistribution.issueSetsWeights.get(issue);
            ValueSet values = actionDistribution.issueSets.get(issue);
            // Idea: https://spinningup.openai.com/en/latest/spinningup/rl_intro3.html
            // 3 Policy functions - Choose issue value a according to each issues value argmax  
            // |
            // | PI_theta(a1|s) = argmax(w1*i1v1 , w2*i1v2 , w3*i1v3)
            // | PI_theta(a2|s) = argmax(w4*i2v1 , w5*i2v2)
            // | PI_theta(a3|s) = argmax(w6*i3v1 , w7*i3v2 , w8*i3v3 , w9*i3v4)
            // |
            // #--> Combine to Bid
            //      Issue: How to connect the different policy gradient functions to interact
            //      | Idea1: Use NN -> Idea already taken!!!
            //      | Idea2: Use Polynomial Linear Regression

        }
        return null;
    }

    /**
     * State
     */
    public class PolicyState {

        private Object representation;

        public PolicyState(AbstractState<?> state) {
            this.representation = state.getRepresentation();
        }

    }

    /**
     * PolicyActionDistribution
     */
    public class PolicyActionDistribution {

        private HashSet<IVPair> ivSets;
        private Map<String, INDArray> issueSetsWeights = new HashMap<>();
        private Map<String, ValueSet> issueSets;

        public PolicyActionDistribution(Domain domain) {
            super();
            ivSets = IVPair.getIVPairMapping(domain).getOrderedIVMapping();
            issueSets = IVPair.getIssueValueSets(domain);
            for (Entry<String, ValueSet> issueSet : issueSets.entrySet()) {
                issueSetsWeights.put(issueSet.getKey(),
                        Nd4j.rand(42l, new long[] { issueSet.getValue().size().longValue() }));
            }
        }
    }
}
