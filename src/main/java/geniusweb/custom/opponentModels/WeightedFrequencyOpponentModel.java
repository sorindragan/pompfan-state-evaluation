package geniusweb.custom.opponentModels;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;

public class WeightedFrequencyOpponentModel extends AbstractOpponentModel {

    public WeightedFrequencyOpponentModel(Domain domain, List<Action> realHistoryActions) {
        super(domain, realHistoryActions);
        this.initializeModel(realHistoryActions);
    }

    private WeightedFrequencyOpponentModel initializeModel(List<Action> realHistoryActions) {
        if (this.getDomain() == null) {
            throw new IllegalStateException("domain is not initialized");
        }

        Set<String> allIssues = this.getDomain().getIssues();
        BigDecimal sum = new BigDecimal(realHistoryActions.size());
        Map<Action, Long> actionCounts = realHistoryActions.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        Map<Action, BigDecimal> actionProbabilities = new HashMap<>();
        actionCounts.forEach((action, cnt) -> actionProbabilities.put(action, new BigDecimal(cnt).divide(sum)));

        Map<String, Map<DiscreteValue, BigDecimal>> newIssueValFreqs = new HashMap<String, Map<DiscreteValue, BigDecimal>>();
        for (Action action : realHistoryActions) {
            if (!(action instanceof Offer))
                continue;

            Bid bid = ((Offer) action).getBid();
            for (String issue : allIssues) {
                Map<DiscreteValue, BigDecimal> freqs = newIssueValFreqs.getOrDefault(issue,
                        new HashMap<DiscreteValue, BigDecimal>());
                DiscreteValue value = (DiscreteValue) bid.getValue(issue);
                if (value != null) {
                    BigDecimal oldfreq = freqs.getOrDefault(value, BigDecimal.ZERO);
                    freqs.put(value, oldfreq.add(BigDecimal.ONE));
                }

            }

        }

        // Normalize counts
        newIssueValFreqs.forEach((issue, valueCounts) -> valueCounts
                .forEach((value, count) -> valueCounts.put(value, count.divide(sum))));
        //

        HashMap<String, ValueSetUtilities> issueUtilities = new HashMap<String, ValueSetUtilities>();
        HashMap<String, BigDecimal> issueWeights = new HashMap<String, BigDecimal>();
        for (String issue : allIssues) {
            Map<DiscreteValue, BigDecimal> map = newIssueValFreqs.get(issue);
            issueWeights.put(issue, WeightedFrequencyOpponentModel.computeEntropy(map));

        }
        BigDecimal issueWeightsSum = issueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        issueWeights.forEach((issue, weight) -> issueWeights.put(issue, weight.divide(issueWeightsSum)));

        for (Entry<String, Map<DiscreteValue, BigDecimal>> issueValues : newIssueValFreqs.entrySet()) {
            issueUtilities.put(issueValues.getKey(), new DiscreteValueSetUtilities(issueValues.getValue()));
        }

        this.setUtilities(issueUtilities);
        this.setIssueWeights(issueWeights);

        return this;
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

    private static BigDecimal computeEntropy(Map<DiscreteValue, BigDecimal> map) {
        return map.values().stream().map(e -> e.multiply(new BigDecimal(Math.log(e.doubleValue()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
