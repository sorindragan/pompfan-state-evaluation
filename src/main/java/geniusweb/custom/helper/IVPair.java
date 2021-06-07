package geniusweb.custom.helper;

import java.util.ArrayList;
import java.util.List;

import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;

public class IVPair {
    String issue;
    Value value;

    public IVPair(String issue2, Value value2) {
        this.issue = issue2;
        this.value = value2;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return issue + ":" + value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return this.toString().contentEquals(obj.toString());
    }

    public static List<IVPair> convertValueSet(String issue, ValueSet values) {
        List<IVPair> pairs = new ArrayList<>();
        for (Value value : values) {
            pairs.add(new IVPair(issue, value));
        }
        return pairs;
    }
}
