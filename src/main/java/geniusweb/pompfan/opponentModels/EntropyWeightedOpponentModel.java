package geniusweb.pompfan.opponentModels;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.Domain;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;

public class EntropyWeightedOpponentModel extends AbstractOpponentModel {

    private PartyId me;

    @JsonCreator
    public EntropyWeightedOpponentModel(@JsonProperty("domain") Domain domain,
            @JsonProperty("history") List<Action> history, @JsonProperty("me") PartyId me) {
        super(domain, history);
        this.setMe(me);
        this.initializeModel(history.stream().filter(action -> !action.getActor().equals(this.getMe()))
                .collect(Collectors.toList()));
    }

    public PartyId getMe() {
        return me;
    }

    public void setMe(PartyId me) {
        this.me = me;
    }

    private EntropyWeightedOpponentModel initializeModel(List<Action> realHistoryActions) {
        if (this.getDomain() == null) {
            throw new IllegalStateException("domain is not initialized");
        }

        Set<String> allIssues = this.getDomain().getIssues();
        BigDecimal sum = new BigDecimal(realHistoryActions.size());

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
                newIssueValFreqs.put(issue, freqs);
            }

        }

        // Normalize counts
        newIssueValFreqs.forEach((issue, valueCounts) -> valueCounts
                .forEach((value, count) -> valueCounts.put(value, count.divide(sum, 5, RoundingMode.HALF_UP))));

        HashMap<String, ValueSetUtilities> issueUtilities = new HashMap<String, ValueSetUtilities>();
        HashMap<String, BigDecimal> issueWeights = new HashMap<String, BigDecimal>();
        for (String issue : allIssues) {
            Map<DiscreteValue, BigDecimal> map = newIssueValFreqs.get(issue);
            BigDecimal weight = BigDecimal.ZERO;
            if (map != null) {
                BigDecimal entropyForIssue = EntropyWeightedOpponentModel.computeEntropy(map);
                // BigDecimal divide = entropyForIssue.compareTo(BigDecimal.ZERO) != 0 ?
                // BigDecimal.ONE.divide(entropyForIssue, 5, RoundingMode.HALF_UP) :
                // BigDecimal.ONE;
                weight = BigDecimal.ONE.subtract(entropyForIssue).max(BigDecimal.ZERO);
            }
            issueWeights.put(issue, weight);

        }
        // BigDecimal issueWeightsSum =
        // issueWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        // BigDecimal E = new BigDecimal("2.71828");
        BigDecimal issueWeightsSum = issueWeights.values().stream().map(val -> Math.pow(Math.E, val.doubleValue()))
                .map(val -> BigDecimal.valueOf(val)).reduce(BigDecimal.ZERO, BigDecimal::add);

        // issueWeights.forEach((issue, weight) -> issueWeights.put(issue,
        // issueWeightsSum.compareTo(BigDecimal.ZERO) != 0 ?
        // weight.divide(issueWeightsSum, 5, RoundingMode.HALF_UP) : BigDecimal.ZERO));
        issueWeights.forEach(
                (issue, weight) -> issueWeights.put(issue, BigDecimal.valueOf(Math.pow(Math.E, weight.doubleValue()))));
        issueWeights.forEach((issue, weight) -> issueWeights.put(issue,
                issueWeightsSum.compareTo(BigDecimal.ZERO) != 0
                        ? weight.divide(issueWeightsSum, 5, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO));

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
        return map.values().stream().map(EntropyWeightedOpponentModel::calcPartialEntropy).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    private static BigDecimal calcPartialEntropy(BigDecimal e) {
        double stabilizer = Math.max(e.doubleValue(), Double.MIN_NORMAL);
        double logP = Math.log(stabilizer);
        String stringLogP = String.valueOf(logP);
        BigDecimal bigDecimalLogP = new BigDecimal(stringLogP);
        BigDecimal result = e.multiply(bigDecimalLogP).multiply(new BigDecimal("-1.0"));
        return result;
    }

}
