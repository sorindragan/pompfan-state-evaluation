package geniusweb.pompfan.opponents;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ImitateOpponentPolicy extends AbstractPolicy {

    public BigDecimal STUBBORNESS = new BigDecimal(new Random().nextDouble());

    private List<Action> recordedBehavior;
    @JsonIgnore
    private FrequencyOpponentModel oppModel = null;

    @JsonCreator
    public ImitateOpponentPolicy(@JsonProperty("domain") Domain domain, @JsonProperty("name") String name,
            @JsonProperty("recordedBehavior") List<Action> realHistoryActions,
            @JsonProperty("STUBBORNESS") BigDecimal STUBBORNESS) {
        super(domain, name);
        this.oppModel = new FrequencyOpponentModel().with(domain, null);
        populateModel(domain, realHistoryActions);

        this.setUtilitySpace(oppModel);
        this.setRecordedBehavior(realHistoryActions);
        // this.STUBBORNESS = STUBBORNESS;

    }

    private void populateModel(Domain domain, List<Action> realHistoryActions) {
        for (Action action : realHistoryActions) {
            ActionWithBid a = (ActionWithBid) action;
            Offer o = new Offer(this.getPartyId(), a.getBid());
            this.oppModel = this.oppModel.with(o, null);
        }
    }

    public ImitateOpponentPolicy(Domain domain, String name, List<Action> realHistoryActions) {
        super(domain, name);
        this.oppModel = new FrequencyOpponentModel().with(domain, null);
        for (Action action : realHistoryActions) {
            this.oppModel = this.oppModel.with(action, null);
        }

        this.setUtilitySpace(this.oppModel);
        this.setRecordedBehavior(realHistoryActions);

    }

    public List<Action> getRecordedBehavior() {
        return recordedBehavior;
    }

    public void setRecordedBehavior(List<Action> recordedBehavior) {
        this.recordedBehavior = recordedBehavior;
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, AbstractState<?> state) {
        Number selectionPoint = Math.floor(this.getRecordedBehavior().size() * state.getTime());
        int selectedIdx = selectionPoint.intValue();
        if (selectedIdx >= this.getRecordedBehavior().size()) {
            if (isGood(lastReceivedBid)) {
                // System.out.println(this.getName() + " generated ACCEPT");
                return new Accept(this.getPartyId(), lastReceivedBid);
            }

            Action selectedAction = this.getRecordedBehavior().get(this.getRecordedBehavior().size() - 1);
            return selectedAction;
        }
        ActionWithBid selectedAction = (ActionWithBid) this.getRecordedBehavior().get(selectedIdx);
        Bid currentBid = selectedAction.getBid();
        Map<String, Value> issuevalues = new HashMap<String, Value>();
        for (String issueString : currentBid.getIssues()) {
            Map<Value, Integer> valCounts = this.getOppModel().getCounts(issueString);
            Integer selected = this.getRandom().nextInt(valCounts.size());
            List<Value> vals = valCounts.keySet().stream().collect(Collectors.toList());
            List<Integer> cnts = valCounts.values().stream().collect(Collectors.toList());
            List<Integer> cumCnts = IntStream.range(0, cnts.size())
                    .map(i -> IntStream.rangeClosed(0, i).map(cnts::get).sum()).boxed().collect(Collectors.toList());

            Integer selectedValIdxTmp = cumCnts.stream().filter(c -> c.compareTo(selected) >= 0).findFirst().get();
            int selectedValIdx = cumCnts.indexOf(selectedValIdxTmp);
            Value selectedVal = vals.get(selectedValIdx);
            issuevalues.put(issueString, selectedVal);
        }
        
        Bid resultBid = this.getRandom().nextDouble() > 0.75 ? new Bid(issuevalues) : currentBid;

        return new Offer(this.getPartyId(), resultBid);
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        return this.chooseAction(lastReceivedBid, state);
    }

    private boolean isGood(Bid bid) {
        return (STUBBORNESS.compareTo(this.getUtilitySpace().getUtility(bid)) < 0);
    }

    @JsonIgnore
    @Override
    public UtilitySpace getUtilitySpace() {
        return super.getUtilitySpace();
    }

    @JsonIgnore
    @Override
    public void setUtilitySpace(UtilitySpace utilitySpace) {
        super.setUtilitySpace(utilitySpace);
    }

    @JsonIgnore
    public FrequencyOpponentModel getOppModel() {
        return this.oppModel;
    }

    @JsonIgnore
    public void setOppModel(FrequencyOpponentModel oppModel) {
        this.oppModel = oppModel;
    }
}
