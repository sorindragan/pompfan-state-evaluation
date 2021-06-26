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

public class ValueApproximationPolicy extends AbstractPolicy {
    private Map<PolicyState, PolicyActionDistribution> policies;
    private List<String> allIssues;

    public ValueApproximationPolicy(Domain domain, String name) {
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
