package geniusweb.custom.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import geniusweb.actions.PartyId;
import geniusweb.custom.beliefs.AbstractBelief;
import geniusweb.custom.beliefs.ParticleFilterBelief;
import geniusweb.custom.beliefs.ParticleFilterWithAcceptBelief;
import geniusweb.custom.beliefs.UniformBelief;
import geniusweb.custom.distances.AbstractBidDistance;
import geniusweb.custom.distances.RandomBidDistance;
import geniusweb.custom.distances.UtilityBidDistance;
import geniusweb.custom.evaluators.IEvalFunction;
import geniusweb.custom.evaluators.Last2BidsMeanUtilityEvaluator;
import geniusweb.custom.evaluators.Last2BidsProductUtilityEvaluator;
import geniusweb.custom.evaluators.RandomEvaluator;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.explorers.HighSelfEsteemOwnExplorationPolicy;
import geniusweb.custom.explorers.RandomNeverAcceptOwnExplorationPolicy;
import geniusweb.custom.explorers.RandomOwnExplorerPolicy;
import geniusweb.custom.explorers.SelfishNeverAcceptOwnExplorerPolicy;
import geniusweb.custom.explorers.SelfishOwnExplorerPolicy;
import geniusweb.custom.explorers.SelfishReluctantOwnExplorerPolicy;
import geniusweb.custom.opponents.AbstractPolicy;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.HistoryState;
import geniusweb.custom.state.UtilityState;
import geniusweb.custom.wideners.AbstractWidener;
import geniusweb.custom.wideners.MaxWidthWideningStrategy;
import geniusweb.custom.wideners.ProgressiveWideningStrategy;
import geniusweb.profile.utilityspace.UtilitySpace;

public class Configurator {
    private String confBidDistance = "UtilityBidDistance";
    private String confBelief = "ParticleFilterWithAcceptBelief";
    private String confEvaluator = "Last2BidsProductUtilityEvaluator";
    private String confInitState = "HistoryState";
    private String confExplorer = "RandomOwnExplorerPolicy";
    private String confWidener = "ProgressiveWideningStrategy";
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

    public String getConfBidDistance() {
        return confBidDistance;
    }

    public void setConfBidDistance(String confBidDistance) {
        this.confBidDistance = confBidDistance;
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
        // this.BidDistance = ;
        return buildBidDistance(this.confBidDistance, this.uSpace)
                .buildBelief(this.confBelief, this.listOfOpponents, this.getBidDistance())
                .buildInitState(this.confEvaluator, this.confInitState, this.uSpace)
                .buildExplorer(this.confExplorer, this.me, this.uSpace)
                .buildWidener(this.confWidener, this.confExtra.get("widener"));
    }

    private Configurator buildBidDistance(String confBidDistance, UtilitySpace uSpace) {
        switch (this.confBidDistance) {
        case "UtilityBidDistance":
            this.setBidDistance(new UtilityBidDistance(uSpace));
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
        case "ParticleFilterBelief":
            this.setBelief(new ParticleFilterBelief(listOfOpponents, distance));
            break;
        case "ParticleFilterWithAcceptBelief":
            this.setBelief(new ParticleFilterWithAcceptBelief(listOfOpponents, distance));
            break;
        default:
            this.setBelief(new UniformBelief(listOfOpponents, distance));
            break;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Configurator buildInitState(String confEvalutator, String confState, UtilitySpace uSpace) {
        switch (confState) {
        case "HistoryState":
            if (confEvalutator == "Last2BidsProductUtilityEvaluator") {
                this.setEvaluator(new Last2BidsProductUtilityEvaluator(uSpace));
            } else if (confEvalutator == "Last2BidsMeanUtilityEvaluator") {
                this.setEvaluator(new Last2BidsMeanUtilityEvaluator(uSpace));
            } else {
                this.setEvaluator(new RandomEvaluator<HistoryState>());
            }
            IEvalFunction<HistoryState> evaluator1 = (IEvalFunction<HistoryState>) this.getEvaluator();
            this.setState(new HistoryState(this.uSpace, null, evaluator1));
            break;
        case "UtilityState":
            this.setEvaluator(new RandomEvaluator<UtilityState>());
            IEvalFunction<UtilityState> evaluator2 = (IEvalFunction<UtilityState>) this.getEvaluator();
            this.setState(new UtilityState(uSpace, null, evaluator2));
            break;
        default:
            // TODO:
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
        default:
            this.setExplorer(new RandomOwnExplorerPolicy(uSpace, me));
            break;
        }
        return this;
    }

    private Configurator buildWidener(String confWidener, HashMap<String, Object> params) {
        switch (confWidener) {
        case "ProgressiveWideningStrategy":
            this.setWidener(new ProgressiveWideningStrategy(this.getExplorer(), params));
            break;
        case "MaxWidthWideningStrategy":
            this.setWidener(new MaxWidthWideningStrategy(this.getExplorer(), params));
            break;
        default:
            // this.setExplorer(new RandomOwnExplorerPolicy(uSpace, me));
            break;
        }
        return this;
    }

    // @SuppressWarnings("unchecked")
    // public <T> T get(String paramname, Class<T> classType) {
    // Class<?> Instance = classType.getClassLoader().loadClass(paramname);
    // // Instance.getDeclaredConstructor().newInstance(initargs)
    // return (T) params.get(paramname);
    // }
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

}
