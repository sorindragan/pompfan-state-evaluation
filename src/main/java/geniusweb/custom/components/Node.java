package geniusweb.custom.components;

import java.util.ArrayList;
import java.util.UUID;

import geniusweb.actions.Action;
import geniusweb.custom.state.AbstractState;
import geniusweb.custom.state.StateRepresentation;
import geniusweb.custom.strategies.AbstractPolicy;

/**
 * Node
 */
public class Node {
    public static enum NODE_TYPE {
        NORMAL, BELIEF, ACTION
    };

    private String id;
    private NODE_TYPE type;
    private Node parent;
    private ArrayList<Node> children;
    private Integer visits = 0;
    private Double value = 0d;
    private AbstractState state;

    public Node(Node parent, AbstractState state) {
        super();
        this.id = UUID.randomUUID().toString();
        this.parent = parent;
        this.state = state;
        this.children = new ArrayList<Node>();

    }

    public NODE_TYPE getType() {
        return type;
    }

    public void setType(NODE_TYPE type) {
        this.type = type;
    }

    public Node addChild(Node childNode) {
        this.children.add(childNode);
        return this;
    }

    public Node updateVisits() {
        this.visits++;
        return this;
    }

    public Node setValue(Double valDouble) {
        this.value = valDouble;
        return this;
    }

    public String getId() {
        return id;
    }

    public Node setId(String id) {
        this.id = id;
        return this;
    }

    public Node getParent() {
        return parent;
    }

    public Node setParent(Node parent) {
        this.parent = parent;
        return this;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public Node setChildren(ArrayList<Node> children) {
        this.children = children;
        return this;
    }

    public Integer getVisits() {
        return visits;
    }

    public Node setVisits(Integer visits) {
        this.visits = visits;
        return this;
    }

    public Double getValue() {
        return value;
    }

    public AbstractState getState() {
        return state;
    }

    public Node setState(AbstractState state) {
        this.state = state;
        return this;
    }

    public static Node buildNode(NODE_TYPE type, Node parent, AbstractState newState,
            AbstractPolicy opponent) {
        
        Node child;
        switch (type) {
            case BELIEF:
                child = new BeliefNode(parent, newState);
                break;
            case ACTION:
                child = new ActionNode(parent, newState);
                break;
            default:
                child = new Node(parent, newState);
                break;
        }
        return child;
    }

}