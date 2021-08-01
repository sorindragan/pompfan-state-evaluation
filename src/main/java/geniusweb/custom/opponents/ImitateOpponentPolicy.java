package geniusweb.custom.opponents;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import geniusweb.actions.Accept;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.custom.opponentModels.FuzzyFreqOpponentModel;
import geniusweb.custom.state.AbstractState;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;

public class ImitateOpponentPolicy extends AbstractPolicy {
    
    
    private final BigDecimal STUBBORNESS = new BigDecimal(0.7f);

    private List<Action> recordedBehavior;
    private FrequencyOpponentModel oppModel = new FrequencyOpponentModel();

    public ImitateOpponentPolicy(Domain domain, String name, List<Action> realHistoryActions) {
        super(domain, name);
        for (Action action : realHistoryActions) {
            oppModel = oppModel.with(action, null);
        }

        this.setUtilitySpace(oppModel);
        this.setRecordedBehavior(realHistoryActions);

    }

    public List<Action> getRecordedBehavior() {
        return recordedBehavior;
    }

    public void setRecordedBehavior(List<Action> recordedBehavior) {
        this.recordedBehavior = recordedBehavior;
    }

    @Override
    public Action chooseAction(Bid lastReceivedBid, Bid lastOwnBid, AbstractState<?> state) {
        Number selectionPoint = Math.floor(this.getRecordedBehavior().size() * state.getTime());
        int selectedIdx = selectionPoint.intValue();
        if (selectedIdx >= this.getRecordedBehavior().size()) {
            if (isGood(lastReceivedBid)) {
                return new Accept(this.getPartyId(), lastReceivedBid);
            }

            ActionWithBid selectedAction = null;
            do {
                selectedAction = (ActionWithBid) this.chooseAction();
            } while (!isGood(selectedAction.getBid()));
            // Action selectedAction = this.getRecordedBehavior().get(cutPoint.intValue());
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
            Integer selectedValIdxTmp = cumCnts.stream().filter(c -> c.compareTo(selected) < 0).findFirst().get();
            int selectedValIdx = cumCnts.indexOf(selectedValIdxTmp);
            Value selectedVal = vals.get(selectedValIdx);
            issuevalues.put(issueString, selectedVal);
        }
        Bid resultBid = new Bid(issuevalues);

        return new Offer(this.getPartyId(), resultBid);
    }

    private boolean isGood(Bid bid) {
        if (STUBBORNESS.compareTo(this.getUtilitySpace().getUtility(bid)) < 0) {
            return true;
        }
        return false;
    }

    public FrequencyOpponentModel getOppModel() {
        return oppModel;
    }

    public void setOppModel(FrequencyOpponentModel oppModel) {
        this.oppModel = oppModel;
    }
}
