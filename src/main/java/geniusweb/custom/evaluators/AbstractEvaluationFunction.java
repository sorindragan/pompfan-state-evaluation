package geniusweb.custom.evaluators;

import geniusweb.custom.state.AbstractState;

public interface AbstractEvaluationFunction {
    public Double evaluate(AbstractState<?> state);
}
