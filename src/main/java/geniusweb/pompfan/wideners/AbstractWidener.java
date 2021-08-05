package geniusweb.pompfan.wideners;

import geniusweb.pompfan.components.Node;
import geniusweb.pompfan.explorers.AbstractOwnExplorationPolicy;
import geniusweb.pompfan.state.StateRepresentationException;
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
