package geniusweb.custom.wideners;

import geniusweb.custom.components.ActionNode;
import geniusweb.custom.components.BeliefNode;
import geniusweb.custom.components.Node;
import geniusweb.custom.components.Tree;
import geniusweb.custom.explorers.AbstractOwnExplorationPolicy;
import geniusweb.custom.state.StateRepresentationException;
import geniusweb.progress.Progress;

public abstract class AbstractWidener {

    private AbstractOwnExplorationPolicy ownExplorationStrategy;
    

    public AbstractWidener(AbstractOwnExplorationPolicy ownExplorationStrategy) {
        this.ownExplorationStrategy = ownExplorationStrategy;
    }

    public AbstractOwnExplorationPolicy getOwnExplorationStrategy() {
        return ownExplorationStrategy;
    }

    public AbstractWidener setOwnExplorationStrategy(AbstractOwnExplorationPolicy ownExplorationStrategy) {
        this.ownExplorationStrategy = ownExplorationStrategy;
        return this;
    }

    public abstract void widen(Progress simulatedProgress, Node currRoot) throws StateRepresentationException;
}
