package geniusweb.custom.state;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.custom.helper.IssueValuePair;
import geniusweb.custom.strategies.AbstractPolicy;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;

public class FrequencyState extends AbstractState {

    HashMap<IssueValuePair, Long> freq;
    private List<IssueValuePair> allIssueValues;

    public FrequencyState(Domain domain, AbstractPolicy opponent) {
        super(domain, opponent);
        this.allIssueValues = domain.getIssues().stream()
                .flatMap(issue -> IssueValuePair.convertValueSet(issue, domain.getValues(issue)).stream())
                .collect(Collectors.toList());
        this.freq = new HashMap<IssueValuePair, Long>();
        for (IssueValuePair value : allIssueValues) {
            this.freq.put(value, 0l);
        }
    }

    public HashMap<IssueValuePair, Long> getFreq() {
        return freq;
    }

    public void setFreq(HashMap<IssueValuePair, Long> freq) {
        this.freq = freq;
    }

    @Override
    public String getStringRepresentation() {
        return this.freq.toString();
    }

    @Override
    public AbstractState updateState(Action nextAction) throws StateRepresentationException {
        Representation representation = new Representation(this.getFreq());
        if (nextAction instanceof Offer) {
            Bid bid = ((Offer) nextAction).getBid();
            List<IssueValuePair> bidIssueValues = bid.getIssueValues().entrySet().stream()
                    .map(entry -> new IssueValuePair(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            for (IssueValuePair issueValuePair : bidIssueValues) {
                representation.computeIfPresent(issueValuePair, (key, val) -> val + 1);
            }
            return new FrequencyState(this.getDomain(), this.getOpponent()).setRepresentation(representation);
        }
        throw new StateRepresentationException();
    }

    @Override
    public AbstractState getCurrentState() {
        return null;
    }

    @Override
    public AbstractState setRepresentation(StateRepresentation<?> representation) {
        Representation customRepr = (Representation) representation;
        this.freq = customRepr.getOriginalObject();
        return this;
    }

    public class Representation extends HashMap<IssueValuePair, Long>
            implements StateRepresentation<HashMap<IssueValuePair, Long>> {

        public Representation(HashMap<IssueValuePair, Long> freq) {
            super(freq);
        }

        @Override
        public HashMap<IssueValuePair, Long> getOriginalObject() {
            return this;
        }

    }

}
