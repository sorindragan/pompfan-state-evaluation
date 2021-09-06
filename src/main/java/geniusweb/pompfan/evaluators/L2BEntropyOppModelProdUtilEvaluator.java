package geniusweb.pompfan.evaluators;

import java.util.ArrayList;

import geniusweb.actions.Action;
import geniusweb.issuevalue.Domain;
import geniusweb.pompfan.opponentModels.EntropyWeightedOpponentModel;
import geniusweb.profile.utilityspace.UtilitySpace;

public class L2BEntropyOppModelProdUtilEvaluator extends L2BAHPOppModelProdUtilEvaluator {

    public L2BEntropyOppModelProdUtilEvaluator(UtilitySpace utilitySpace) {
        super(utilitySpace);
    }

    public L2BEntropyOppModelProdUtilEvaluator() {
    }

    
    @Override
    protected void initOppModel(ArrayList<Action> currState, Domain domain) {
        this.setOppModel(new EntropyWeightedOpponentModel(domain, currState, this.getHolder()));
    }

}
