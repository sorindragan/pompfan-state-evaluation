package geniusweb.pompfan.opponentModels;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;

public class BetterFreqOppModel extends AbstractOpponentModel {
    private FrequencyOpponentModel subFreqModel;
    private PartyId me;
    private Set<String> allIssues;
    private Map<String, Double> iChanges = new HashMap<>();

    public BetterFreqOppModel(Domain domain, List<Action> history, PartyId me) {
        super(domain, history);
        this.me = me;
        this.allIssues = this.getDomain().getIssues();

        this.subFreqModel = new FrequencyOpponentModel().with(domain, null);
        // Map<String, Map<Value, Integer>> table = new HashMap<>();
        // for (String issue : allIssues) {
        // Map<Value, Integer> vSet = new HashMap<>();
        // ValueSet allValues = this.getDomain().getValues(issue);
        // for (Value value1 : allValues) {
        // DiscreteValue v1 = (DiscreteValue) value1;
        // vSet.put(v1, 1);
        // }
        // table.put(issue, vSet);
        // }
        // this.subFreqModel = new FrequencyOpponentModel(domain, table);

        for (String issue1 : allIssues) {
            this.iChanges.put(issue1, 1.0 / allIssues.size());
        }
        if (history != null && history.size() > 4) {
            this.initializeModel(history);
        }
    }

    private BetterFreqOppModel initializeModel(List<Action> history) {
        List<Action> myActions = history.stream().filter(action -> action.getActor().equals(this.me))
                .collect(Collectors.toList()); // Us
        List<Action> oppActions = history.stream().filter(action -> !action.getActor().equals(this.me))
                .collect(Collectors.toList()); // Opponent
        Integer numPairs = Math.min(myActions.size(), oppActions.size());
        Bid lastBid = ((ActionWithBid) oppActions.get(0)).getBid();
        for (int i = 0; i < numPairs; i++) {
            Bid firstBid = ((ActionWithBid) myActions.get(i)).getBid();
            Action oppAction = oppActions.get(i);
            Bid secondBid = ((ActionWithBid) oppAction).getBid();
            for (String nextIssue : this.getDomain().getIssues()) {
                DiscreteValue firstVal = (DiscreteValue) firstBid.getValue(nextIssue);
                DiscreteValue secondVal = (DiscreteValue) secondBid.getValue(nextIssue);
                DiscreteValue lastOpponentValue = (DiscreteValue) lastBid.getValue(nextIssue);
                if (secondVal.equals(lastOpponentValue)) {
                    this.iChanges = incrementIssueTable(nextIssue, this.iChanges);
                }
            }
            this.subFreqModel = this.subFreqModel.with(oppAction, null);
            lastBid = secondBid;
        }
        this.updateWeights(this.iChanges, this.subFreqModel);
        return this;
    }

    private void updateWeights(Map<String, Double> iChanges2, FrequencyOpponentModel subFreqModel2) {
        Double sumIssueRepeats = iChanges2.values().parallelStream().collect(Collectors.summingDouble(d -> d));
        Set<String> allIssues = this.getDomain().getIssues();
        HashMap<String, ValueSetUtilities> issueUtilities = new HashMap<String, ValueSetUtilities>();
        HashMap<String, BigDecimal> issueWeights = new HashMap<String, BigDecimal>();

        for (String issue : allIssues) {
            Double weight = iChanges2.get(issue) / sumIssueRepeats;
            issueWeights.put(issue, new BigDecimal(weight));

            Map<DiscreteValue, BigDecimal> issueValues = new HashMap<>();
            Map<Value, Double> allCounts = this.subFreqModel.getCounts(issue).entrySet().parallelStream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue() * 1.0));
            List<DiscreteValue> allValues = StreamSupport.stream(this.getDomain().getValues(issue).spliterator(), false)
                    .map(v -> (DiscreteValue) v).collect(Collectors.toList());

            Map<Value, Double> completeAllCounts = allValues.stream().map(v -> new SimpleEntry<>(v, allCounts.get(v)))
                    .map(cnt -> cnt.getValue() != null ? cnt : new SimpleEntry<>(cnt.getKey(), 1.0))
                    // .map(cnt -> new SimpleEntry<>(cnt.getKey(), Math.log(cnt.getValue())))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            Double sumValues = completeAllCounts.values().parallelStream().collect(Collectors.summingDouble(d -> d));

            issueValues = completeAllCounts.entrySet().stream()
                    .map(cnt -> new SimpleEntry<>(cnt.getKey(), cnt.getValue() / sumValues))
                    .collect(Collectors.toMap(e -> (DiscreteValue) e.getKey(), e -> new BigDecimal(e.getValue())));
            // sumValues = Math.log(sumValues + 0.0000000000001);

            // for (DiscreteValue v : allValues) {
            // Integer vCntInt = allCounts.get(v);
            // Double vCnt = vCntInt != null ? vCntInt * 1.0 : 0.0;
            // // vCnt = Math.log(vCnt + 0.0000000000001) ;
            // issueValues.put(v, new BigDecimal(vCnt / sumValues));
            // }
            issueUtilities.put(issue, new DiscreteValueSetUtilities(issueValues));

        }
        this.setUtilities(issueUtilities);
        this.setIssueWeights(issueWeights);
    }

    private Map<String, Double> incrementIssueTable(String nextIssue, Map<String, Double> mapOfConsecutiveChanges) {
        Double consecutiveChanges = mapOfConsecutiveChanges.get(nextIssue);
        Double currCnt = consecutiveChanges == null ? 1.0 : consecutiveChanges + 1.0;
        mapOfConsecutiveChanges.put(nextIssue, currCnt);
        return mapOfConsecutiveChanges;
    }

    @Override
    public BigDecimal getUtility(Bid bid) {
        return this.getWeights().keySet().stream().map(iss -> this.util(iss, bid.getValue(iss))).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public OpponentModel with(Domain domain, Bid resBid) {
        return this;
    }

    @Override
    public OpponentModel with(Action action, Progress progress) {
        this.getHistory().add(action);
        return this.initializeModel(this.getHistory());
    }

}
