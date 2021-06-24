package geniusweb.custom.helper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.datavec.api.records.mapper.RecordMapper;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.collection.ListStringRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.records.reader.impl.transform.TransformProcessRecordReader;
import org.datavec.api.split.ListStringSplit;
import org.datavec.api.split.StringSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.transform.transform.categorical.CategoricalToIntegerTransform;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.iterator.RandomDataSetIterator.Values;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.shape.OneHot;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;

public class IVPair {
    String issue;
    Value value;
    private static IVPairMapping ivPairMapping;

    public IVPair(String issue2, Value value2) {
        this.issue = issue2;
        this.value = value2;
    }

    public IVPair(String issue2, String value2) {
        this.issue = issue2;
        this.value = new DiscreteValue(value2);
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

    public Bid getPartialBid() {
        return new Bid(this.issue, this.value);
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

    public static List<String> convertValueSet(ValueSet values) {
        List<String> valsStrings = new ArrayList<String>();
        for (Value value : values) {
            valsStrings.add(value.toString());
        }
        return valsStrings;
    }

    public static Map<String, ValueSet> getIssueValueSets(Domain domain) {
        return IVPair.getIVPairMapping(domain).getIssueValueSets();
    }

    public static List<String> getIssues(Domain domain) {
        return IVPair.getIVPairMapping(domain).getIssueList();
    }

    public static HashMap<IVPair, Double> getVectorContainer(Domain domain) {
        HashMap<IVPair, Double> container = new HashMap<IVPair, Double>();
        IVPairMapping allIssueValuesOrdered = IVPair.getIVPairMapping(domain);
        for (IVPair value : allIssueValuesOrdered.getMapping()) {
            container.put(value, 0.0);
        }
        return container;
    }

    public static IVPairMapping getIVPairMapping(Domain domain) {
        if (IVPair.ivPairMapping == null) {
            IVPair.ivPairMapping = new IVPairMapping(domain);
        }
        return IVPair.ivPairMapping;
    }

    public static INDArray getVector(IVPair ivPair, Domain domain) {
        IVPairMapping mapping = IVPair.getIVPairMapping(domain);
        INDArray vector = mapping.getVector(ivPair);
        return vector;
    }

    public static INDArray getContainerVector(Domain domain) {
        return IVPair.getIVPairMapping(domain).getContainerVector().dup();
    }

    /**
     * IVPairMapping
     */
    public static class IVPairMapping extends ArrayList<IVPair> {
        // private Schema schema;
        // private TransformProcess encoder;
        // public final String IV_COL = "IssueValues";
        private INDArray matrixArray;
        private HashMap<String, ValueSet> issueValueSets = new HashMap<>();
        private List<String> issueList;
        private HashSet<IVPair> allIVMapping;
        private INDArray containerVector;
        private List<INDArray> base_vectors;

        public IVPairMapping(Domain domain) {
            super();

            this.issueList = new ArrayList<String>(domain.getIssues());
            for (String issue : issueList) {
                ValueSet values = domain.getValues(issue);
                this.issueValueSets.put(issue, values);
                List<IVPair> vals = IVPair.convertValueSet(issue, values);
                vals.remove(0);
                this.addAll(vals);
            }

            this.allIVMapping = new HashSet<IVPair>(domain.getIssues().stream()
                    .flatMap(issue -> IVPair.convertValueSet(issue, domain.getValues(issue)).stream())
                    .collect(Collectors.toSet()));

            this.setMatrixArray(Nd4j.eye(this.size() - 1));
            // long length = this.size()-this.issueList.size();
            // long[] shape = { length, 1l };
            // this.setContainerVector(Nd4j.zeros(length).reshape(shape));

            this.base_vectors = this.issueValueSets.entrySet().stream().map(ivSet -> Nd4j.zeros(ivSet.getValue().size().intValue()-1)).collect(Collectors.toList());
            this.setContainerVector(Nd4j.toFlattened(this.base_vectors));;
            this.addAll(allIVMapping);
        }

        public INDArray getMatrixArray() {
            return matrixArray;
        }

        private void setMatrixArray(INDArray matrixArray) {
            this.matrixArray = matrixArray;
        }

        public ArrayList<IVPair> getMapping() {
            return this;
        }

        public INDArray getContainerVector() {
            return this.containerVector;
        }

        private void setContainerVector(INDArray containerVector) {
            this.containerVector = containerVector;
        }

        public INDArray getVector(IVPair ivp) {
            int idx = this.indexOf(ivp);
            if (idx!=-1) {
                this.containerVector.dup().put(idx, Nd4j.create(1));
            }
            return this.containerVector.dup();
        }

        public HashMap<String, ValueSet> getIssueValueSets() {
            return issueValueSets;
        }

        public void setIssueValueSets(HashMap<String, ValueSet> issueValueSets) {
            this.issueValueSets = issueValueSets;
        }

        public List<String> getIssueList() {
            return issueList;
        }

        public void setIssueList(List<String> issueList) {
            this.issueList = issueList;
        }

        public HashSet<IVPair> getOrderedIVMapping() {
            return allIVMapping;
        }

        public void setOrderedIVMapping(HashSet<IVPair> orderedIVMapping) {
            this.allIVMapping = orderedIVMapping;
        };

    }

    public static class BidMapping {
        private Schema schema;
        private TransformProcess encoder;
        private RecordReader transformProcessRecordReader;

        public BidMapping(Domain domain) throws IOException, InterruptedException {
            super();

            Set<String> issues = domain.getIssues();
            Map<String, ValueSet> ivs = IVPair.getIssueValueSets(domain);
            ArrayList<IVPair> bids = IVPair.getIVPairMapping(domain).getMapping();
            List<List<String>> bidValStrings = new ArrayList<>();

            Builder schemaBuilder = new Schema.Builder();
            for (Entry<String, ValueSet> ivEntry : ivs.entrySet()) {
                List<String> valueSetList = IVPair.convertValueSet(ivEntry.getValue());
                schemaBuilder.addColumnCategorical(ivEntry.getKey(), valueSetList);
            }
            for (Bid bid : this.generateBidspace(domain)) {
                List<String> valString = bid.getIssueValues().entrySet().stream()
                        .map(entry -> entry.getValue().toString()).collect(Collectors.toList());
                bidValStrings.add(valString);
            }
            this.schema = schemaBuilder.build();
            org.datavec.api.transform.TransformProcess.Builder encoderBuilder = new TransformProcess.Builder(schema);
            for (String iss : issues) {
                String remove = domain.getValues(iss).iterator().next().toString();
                encoderBuilder.categoricalToOneHot(iss).removeColumns(iss + "[" + remove + "]");
            }
            this.encoder = encoderBuilder.build();

            /*
             * first line to skip and // comma seperated
             */
            RecordReader listReader = new ListStringRecordReader();
            listReader.initialize(new ListStringSplit(bidValStrings));
            this.transformProcessRecordReader = new TransformProcessRecordReader(listReader, this.encoder);
        }

        public List<Writable> transformBid(Bid bid) {
            List<String> valString = bid.getIssueValues().entrySet().stream().map(entry -> entry.getValue().toString())
                    .collect(Collectors.toList());
            List<Writable> transformed = this.encoder.transformRawStringsToInputList(valString);
            return transformed;
        }

        public List<Bid> generateBidspace(Domain domain) {
            List<String> issueList = IVPair.getIssues(domain);
            List<List<IVPair>> bidspace = recurseBidSpace(issueList, domain, 0);
            List<Bid> allBids = new ArrayList<>();
            for (List<IVPair> bid : bidspace) {
                List<Bid> partialBids = bid.stream().map(partialBid -> partialBid.getPartialBid())
                        .collect(Collectors.toList());
                Bid start = partialBids.get(0);
                partialBids.remove(start);
                for (Bid bid2 : partialBids) {
                    start = start.merge(bid2);
                }
                allBids.add(start);
            }
            return allBids;
        }

        private List<List<IVPair>> recurseBidSpace(List<String> issueList, Domain domain, Integer idx) {
            if (idx > issueList.size()) {
                List<List<IVPair>> collector = new ArrayList<List<IVPair>>();
                ArrayList<IVPair> endList = new ArrayList<IVPair>();
                String issue = issueList.get(idx);
                for (String val : IVPair.convertValueSet(domain.getValues(issue))) {
                    endList.add(new IVPair(issue, val));
                }
                collector.add(endList);
                return collector;
            }

            List<List<IVPair>> collector = new ArrayList<List<IVPair>>();
            String issue = issueList.get(idx);
            for (String val1 : IVPair.convertValueSet(domain.getValues(issue))) {
                collector.addAll(recurseBidSpace(issueList, domain, idx + 1));
            }
            return collector;
        };
    }

}
