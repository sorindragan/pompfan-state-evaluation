package geniusweb.pompfan.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import geniusweb.actions.PartyId;
import geniusweb.pompfan.beliefs.AbstractBelief;
import geniusweb.pompfan.beliefs.BayesianCountFilterBelief;
import geniusweb.pompfan.beliefs.BayesianParticleFilterBelief;
import geniusweb.pompfan.beliefs.ParticleFilterBelief;
import geniusweb.pompfan.beliefs.ParticleFilterWithAcceptBelief;
import geniusweb.pompfan.beliefs.UniformBelief;
import geniusweb.pompfan.distances.AbstractBidDistance;
import geniusweb.pompfan.distances.BothUtilityBidDistance;
import geniusweb.pompfan.distances.ExactSameBidDistance;
import geniusweb.pompfan.distances.IssueValueCountBidDistance;
import geniusweb.pompfan.distances.JaccardBidDistance;
import geniusweb.pompfan.distances.OppUtilityBidDistance;
import geniusweb.pompfan.distances.RandomBidDistance;
import geniusweb.pompfan.distances.SDiceBidDistance;
import geniusweb.pompfan.distances.UtilityBidDistance;
import geniusweb.pompfan.evaluators.IEvalFunction;
import geniusweb.pompfan.evaluators.Last2BidsMixtProdUtilEvaluator;
import geniusweb.pompfan.evaluators.Last2BidsOneMinusDifferenceUtilityEvaluator;
import geniusweb.pompfan.evaluators.Last2BidsMeanUtilityEvaluator;
import geniusweb.pompfan.evaluators.Last2BidsMixtOneMinusDifferenceUtilEvaluator;
import geniusweb.pompfan.evaluators.Last2BidsMixtMeanUtilityEvaluator;
import geniusweb.pompfan.evaluators.Last2BidsProductUtilityEvaluator;
import geniusweb.pompfan.evaluators.RandomEvaluator;
import geniusweb.pompfan.evaluators.others.L2BAHPOppModelProdUtilEvaluator;
import geniusweb.pompfan.evaluators.others.L2BEntropyOppModelProdUtilEvaluator;
import geniusweb.pompfan.evaluators.others.L2BFreqOppModelProdUtilEvaluator;
import geniusweb.pompfan.evaluators.others.OppConcessionUtilityEvaluator;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.explorers.HighSelfEsteemOwnExplorationPolicy;
import geniusweb.pompfan.explorers.RandomNeverAcceptOwnExplorationPolicy;
import geniusweb.pompfan.explorers.RandomOwnExplorerPolicy;
import geniusweb.pompfan.explorers.SelfishNeverAcceptOwnExplorerPolicy;
import geniusweb.pompfan.explorers.SelfishOwnExplorerPolicy;
import geniusweb.pompfan.explorers.SelfishReluctantOwnExplorerPolicy;
import geniusweb.pompfan.explorers.TimeConcedingExplorationPolicy;
import geniusweb.pompfan.particles.AbstractPolicy;
import geniusweb.pompfan.state.AbstractState;
import geniusweb.pompfan.state.HistoryState;
import geniusweb.pompfan.wideners.AbstractWidener;
import geniusweb.pompfan.wideners.MaxWidthWideningStrategy;
import geniusweb.pompfan.wideners.ProgressiveWideningStrategy;
import geniusweb.profile.utilityspace.UtilitySpace;

public class Configurator {

    /**
     *
     */
    public static final String DEFAULT_COMPARER = "UtilityBidDistance";
    public static final String DEFAULT_BELIEF = "ParticleFilterWithAcceptBelief";
    public static final String DEFAULT_EVALUATOR = "Last2BidsProductUtilityEvaluator";
    public static final String DEFAULT_STATE = "HistoryState";
    public static final String DEFAULT_EXPLORER_POLICY = "TimeConcedingExplorationPolicy";
    public static final String DEFAULT_WIDENING_STRATEGY = "ProgressiveWideningStrategy";
    private String confComparer = DEFAULT_COMPARER;
    private String confBelief = DEFAULT_BELIEF;
    private String confEvaluator = DEFAULT_EVALUATOR;
    private String confInitState = DEFAULT_STATE;
    private String confExplorer = DEFAULT_EXPLORER_POLICY;
    private String confWidener = DEFAULT_WIDENING_STRATEGY;
    private HashMap<String, HashMap<String, Object>> confExtra = new HashMap<String, HashMap<String, Object>>();

    private UtilitySpace uSpace = null;
    private PartyId me = null;
    private List<AbstractPolicy> listOfOpponents = new ArrayList<AbstractPolicy>();

    private AbstractBidDistance BidDistance = null;
    private AbstractBelief Belief = null;
    private IEvalFunction<?> Evaluator = null;
    private AbstractState<?> State = null;
    private AbstractOwnExplorationPolicy Explorer = null;
    private AbstractWidener Widener = null;

    public Configurator() {
        this.confExtra.put("widener", new HashMap<String, Object>());
        this.confExtra.put("comparer", new HashMap<String, Object>());
        this.confExtra.put("explorer", new HashMap<String, Object>());
    }

    public HashMap<String, HashMap<String, Object>> getConfExtra() {
        return confExtra;
    }

    public void setConfExtra(HashMap<String, HashMap<String, Object>> confExtra) {
        this.confExtra = confExtra;
    }

    public UtilitySpace getuSpace() {
        return uSpace;
    }

    public void setuSpace(UtilitySpace uSpace) {
        this.uSpace = uSpace;
    }

    public PartyId getMe() {
        return me;
    }

    public Configurator setMe(PartyId me) {
        this.me = me;
        return this;
    }

    public List<AbstractPolicy> getListOfOpponents() {
        return listOfOpponents;
    }

    public Configurator setListOfOpponents(List<AbstractPolicy> listOfOpponents) {
        this.listOfOpponents = listOfOpponents;
        return this;
    }

    public UtilitySpace getUtilitySpace() {
        return uSpace;
    }

    public Configurator setUtilitySpace(UtilitySpace utilitySpace) {
        this.uSpace = utilitySpace;
        return this;
    }

    public String getConfComparer() {
        return confComparer;
    }

    public void setConfComparer(String confBidDistance) {
        this.confComparer = confBidDistance;
    }

    public String getConfBelief() {
        return confBelief;
    }

    public void setConfBelief(String confBelief) {
        this.confBelief = confBelief;
    }

    public String getConfEvaluator() {
        return confEvaluator;
    }

    public void setConfEvaluator(String confEvaluator) {
        this.confEvaluator = confEvaluator;
    }

    public String getConfState() {
        return confInitState;
    }

    public void setConfState(String confState) {
        this.confInitState = confState;
    }

    public String getConfExplorer() {
        return confExplorer;
    }

    public void setConfExplorer(String confExplorer) {
        this.confExplorer = confExplorer;
    }

    public String getConfWidener() {
        return confWidener;
    }

    public void setConfWidener(String confWidener) {
        this.confWidener = confWidener;
    }

    public Configurator build() {

        return buildBidDistance(this.confComparer, this.uSpace)
                .buildBelief(this.confBelief, this.listOfOpponents, this.getBidDistance())
                .buildInitState(this.confEvaluator, this.confInitState, this.uSpace, this.me)
                .buildExplorer(this.confExplorer, this.me, this.uSpace)
                .buildWidener(this.confWidener, this.confExtra.get("widener"));
    }

    private Configurator buildBidDistance(String confBidDistance, UtilitySpace uSpace) {
        switch (this.confComparer) {
        case DEFAULT_COMPARER:
            this.setBidDistance(new UtilityBidDistance(uSpace));
            break;
        case "OppUtilityBidDistance":
            this.setBidDistance(new OppUtilityBidDistance(uSpace));
            break;
        case "BothUtilityBidDistance":
            this.setBidDistance(new BothUtilityBidDistance(uSpace));
            break;
        case "SDiceBidDistance":
            this.setBidDistance(new SDiceBidDistance(uSpace));
            break;
        case "JaccardBidDistance":
            this.setBidDistance(new JaccardBidDistance(uSpace));
            break;
        case "ExactSameBidDistance":
            this.setBidDistance(new ExactSameBidDistance(uSpace));
            break;
        case "IssueValueCountBidDistance":
            this.setBidDistance(new IssueValueCountBidDistance(uSpace));
            break;
        default:
            this.setBidDistance(new RandomBidDistance(uSpace));
            break;
        }
        return this;
    }

    private Configurator buildBelief(String confBelief, List<AbstractPolicy> listOfOpponents,
            AbstractBidDistance distance) {
        switch (confBelief) {
        case DEFAULT_BELIEF:
            this.setBelief(new ParticleFilterWithAcceptBelief(listOfOpponents, distance));
            break;
        case "ParticleFilterBelief":
            this.setBelief(new ParticleFilterBelief(listOfOpponents, distance));
            break;
        case "BayesianCountFilterBelief":
            this.setBelief(new BayesianCountFilterBelief(listOfOpponents, distance));
            break;
        case "BayesianParticleFilterBelief":
            this.setBelief(new BayesianParticleFilterBelief(listOfOpponents, distance));
            break;
        default:
            this.setBelief(new UniformBelief(listOfOpponents, distance));
            break;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Configurator buildInitState(String confEvalutator, String confState, UtilitySpace uSpace, PartyId me) {
        switch (confState) {
        case DEFAULT_STATE:
            if (confEvalutator.equals(DEFAULT_EVALUATOR)) {
                this.setEvaluator(new Last2BidsProductUtilityEvaluator(uSpace));
            } else if (confEvalutator.equals("Last2BidsMeanUtilityEvaluator")) {
                this.setEvaluator(new Last2BidsMeanUtilityEvaluator(uSpace));
            } else if (confEvalutator.equals("Last2BidsOneMinusDifferenceUtilityEvaluator")) {
                this.setEvaluator(new Last2BidsOneMinusDifferenceUtilityEvaluator(uSpace));
            } else if (confEvalutator.equals("Last2BidsMixtMeanUtilityEvaluator")) {
                this.setEvaluator(new Last2BidsMixtMeanUtilityEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("Last2BidsMixtProdUtilEvaluator")) {
                this.setEvaluator(new Last2BidsMixtProdUtilEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("Last2BidsMixtOneMinusDifferenceUtilEvaluator")) {
                this.setEvaluator(new Last2BidsMixtOneMinusDifferenceUtilEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("L2BFreqOppModelProdUtilEvaluator")) {
                this.setEvaluator(new L2BFreqOppModelProdUtilEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("L2BAHPOppModelProdUtilEvaluator")) {
                this.setEvaluator(new L2BAHPOppModelProdUtilEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("L2BEntropyOppModelProdUtilEvaluator")) {
                this.setEvaluator(new L2BEntropyOppModelProdUtilEvaluator(uSpace).setHolder(me));
            } else if (confEvalutator.equals("OppConcessionUtilityEvaluator")) {
                this.setEvaluator(new OppConcessionUtilityEvaluator(uSpace).setHolder(me));
            } else {
                this.setEvaluator(new RandomEvaluator());
            }
            IEvalFunction<HistoryState> evaluator_ = (IEvalFunction<HistoryState>) this.getEvaluator();
            this.setState(new HistoryState(this.uSpace, null, evaluator_));
            this.getInitState().setTime(0.0);
            break;
        default:
            // ??
            System.out.println("ERRRR!");
            break;
        }
        return this;
    }

    private Configurator buildExplorer(String confExplorer, PartyId me, UtilitySpace uSpace) {
        switch (confExplorer) {
        case "HighSelfEsteemOwnExplorationPolicy":
            this.setExplorer(new HighSelfEsteemOwnExplorationPolicy(uSpace, me));
            break;
        case "RandomNeverAcceptOwnExplorationPolicy":
            this.setExplorer(new RandomNeverAcceptOwnExplorationPolicy(uSpace, me));
            break;
        case "SelfishNeverAcceptOwnExplorerPolicy":
            this.setExplorer(new SelfishNeverAcceptOwnExplorerPolicy(uSpace, me));
            break;
        case "SelfishOwnExplorerPolicy":
            this.setExplorer(new SelfishOwnExplorerPolicy(uSpace, me));
            break;
        case "SelfishReluctantOwnExplorerPolicy":
            this.setExplorer(new SelfishReluctantOwnExplorerPolicy(uSpace, me));
            break;
        case DEFAULT_EXPLORER_POLICY:
            this.setExplorer(new TimeConcedingExplorationPolicy(uSpace, me));
            break;
        default:
            this.setExplorer(new RandomOwnExplorerPolicy(uSpace, me));
            break;
        }
        return this;
    }

    private Configurator buildWidener(String confWidener, HashMap<String, Object> params) {
        switch (confWidener) {
        case DEFAULT_WIDENING_STRATEGY:
            this.setWidener(new ProgressiveWideningStrategy(this.getExplorer(), params));
            break;
        case "MaxWidthWideningStrategy":
            this.setWidener(new MaxWidthWideningStrategy(this.getExplorer(), params));
            break;
        default:
            break;
        }
        return this;
    }

    public AbstractBidDistance getBidDistance() {
        return BidDistance;
    }

    public void setBidDistance(AbstractBidDistance bidDistance) {
        BidDistance = bidDistance;
    }

    public AbstractBelief getBelief() {
        return Belief;
    }

    public void setBelief(AbstractBelief belief) {
        Belief = belief;
    }

    public IEvalFunction<?> getEvaluator() {
        return Evaluator;
    }

    public void setEvaluator(IEvalFunction<?> evaluator) {
        Evaluator = evaluator;
    }

    public AbstractState<?> getInitState() {
        return State;
    }

    public void setState(AbstractState<?> state) {
        State = state;
    }

    public AbstractOwnExplorationPolicy getExplorer() {
        return Explorer;
    }

    public void setExplorer(AbstractOwnExplorationPolicy explorer) {
        Explorer = explorer;
    }

    public AbstractWidener getWidener() {
        return Widener;
    }

    public void setWidener(AbstractWidener widener) {
        Widener = widener;
    }

    public static HashMap<String, Object> generateDefaultConfig() {
        HashMap<String, Object> tmp = new HashMap<String, Object>();
        tmp.put("confComparer", DEFAULT_COMPARER);
        tmp.put("confBelief", DEFAULT_BELIEF);
        tmp.put("confEvaluator", DEFAULT_EVALUATOR);
        tmp.put("confState", DEFAULT_STATE);
        tmp.put("confExplorer", DEFAULT_EXPLORER_POLICY);
        tmp.put("confWidener", DEFAULT_WIDENING_STRATEGY);
        return tmp;
    }

}
