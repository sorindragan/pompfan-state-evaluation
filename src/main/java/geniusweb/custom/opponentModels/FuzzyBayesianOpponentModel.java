package geniusweb.custom.opponentModels;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.actions.PartyId;
import geniusweb.bidspace.AllBidsList;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;
import geniusweb.progress.Progress;
import tudelft.utilities.immutablelist.AbstractImmutableList;

// TODO: Generic impl. of random generator
// TODO: Baysian version
// TODO: Time dependant
public class FuzzyBayesianOpponentModel implements UtilitySpace, OpponentModel, IFuzzyModel {

    private static final int SLACK = 3;
    private Map<String, List<Value>> bidFreqAsList = null;
    private Integer initBidCount = 0;
    private Map<String, Map<Value, Long>> sumIssVals = new HashMap<String, Map<Value, Long>>();
    @JsonIgnore
    private Random random = new Random();
    private int numGenerations = 10;
    private PartyId partyId;
    private List<Action> newHistory;

    public FuzzyBayesianOpponentModel(PartyId actor, List<Action> realHistoryActions) {
        super();
        this.setPartyId(actor);;
        this.setInitBidCount(realHistoryActions.size());
        this.setNumGenerations(Math.max(0, this.getInitBidCount() + (this.random.nextInt(SLACK) - (2 * SLACK))));;
        this.setNewHistory(this.genNewFreqModel(realHistoryActions));
        // this.setBidFrequencies();;

    }

    public List<Action> getNewHistory() {
        return newHistory;
    }

    public void setNewHistory(List<Action> newHistory) {
        this.newHistory = newHistory;
    }



    public List<Action> genNewFreqModel(List<Action> realHistoryActions) {
        if (this.getDomain() == null) {
            throw new IllegalStateException("domain is not initialized");
        }
        // cutPoint = 
        List<Action> newFreqs = new ArrayList<Action>();
        for (Action action : realHistoryActions) {
            if (!(action instanceof Offer))
            continue;
            newFreqs.add(action);
            List<Action> genFreqs = new ArrayList<Action>();
            for (int i = 0; i < 100; i++) {
                int selectedIdx = this.getRandom().nextInt(newFreqs.size());
                genFreqs.add(newFreqs.get(selectedIdx));
            }
            int selectedIdx = this.getRandom().nextInt(genFreqs.size());
            newFreqs.remove(newFreqs.size()-1);
            newFreqs.add(genFreqs.get(selectedIdx));
        }
        
        return newFreqs;
    }

    @Override
    public List<Action> generateHistory() {
        List<Action> newHist = new ArrayList<Action>();
        Set<String> allIssues = this.sumIssVals.keySet();
        for (int i = 0; i < this.numGenerations; i++) {
            Map<String, Value> tmpMap = new HashMap<String, Value>();
            Bid bid = null;
            for (String issue : allIssues) {
                List<Value> candidates = this.bidFreqAsList.get(issue);
                Integer selectedIndex = this.random.nextInt(this.initBidCount);
                Value selectedValue = candidates.get(selectedIndex);
                tmpMap.put(issue, selectedValue);
                bid = new Bid(tmpMap);
            }
            newHist.add(new Offer(this.partyId, bid));
        }
        return newHist;
    }

    public Integer getInitBidCount() {
        return initBidCount;
    }

    public void setInitBidCount(Integer initBidCount) {
        this.initBidCount = initBidCount;
    }

    public Map<String, List<Value>> getBidFrequencies() {
        return bidFreqAsList;
    }

    public void setBidFrequencies(Map<String, List<Value>> bidFrequencies) {
        this.bidFreqAsList = bidFrequencies;
    }

    public Map<String, Map<Value, Long>> getSumIssVals() {
        return sumIssVals;
    }

    public void setSumIssVals(Map<String, Map<Value, Long>> sumIssVals) {
        this.sumIssVals = sumIssVals;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public int getNumGenerations() {
        return numGenerations;
    }

    public void setNumGenerations(int numGenerations) {
        this.numGenerations = numGenerations;
    }

    public PartyId getPartyId() {
        return partyId;
    }

    public void setPartyId(PartyId partyId) {
        this.partyId = partyId;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Domain getDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bid getReservationBid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpponentModel with(Domain domain, Bid resBid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OpponentModel with(Action action, Progress progress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public BigDecimal getUtility(Bid bid) {
        // TODO Auto-generated method stub
        return null;
    }
}
