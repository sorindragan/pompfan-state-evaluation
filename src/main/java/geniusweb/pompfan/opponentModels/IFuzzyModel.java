package geniusweb.pompfan.opponentModels;

import java.util.List;

import geniusweb.actions.Action;

public interface IFuzzyModel {
    public List<Action> realHistoryActions = null;
    public Double noiseRatio = 0.1;
    public List<Action> generateHistory();
}
