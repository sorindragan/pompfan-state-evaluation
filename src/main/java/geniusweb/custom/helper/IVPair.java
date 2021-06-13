package geniusweb.custom.helper;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.datavec.api.records.mapper.RecordMapper;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.collection.ListStringRecordReader;
import org.datavec.api.split.ListStringSplit;
import org.datavec.api.split.StringSplit;
import org.datavec.api.transform.TransformProcess;
import org.datavec.api.transform.schema.Schema;
import org.datavec.api.transform.schema.Schema.Builder;
import org.datavec.api.transform.transform.categorical.CategoricalToIntegerTransform;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.iterator.RandomDataSetIterator.Values;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
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
        Integer idx = mapping.indexOf(ivPair);
        INDArray vector = mapping.getVector(idx);
        return vector;
    }

    /**
     * IVPairMapping
     */
    public static class IVPairMapping extends ArrayList<IVPair> {
        // private Schema schema;
        // private TransformProcess encoder;
        // public final String IV_COL = "IssueValues";
        private INDArray matrixArray;
        private INDArray containerVector;

        public IVPairMapping(Domain domain) {
            super();

            Set<IVPair> unorderedMapping = domain.getIssues().stream()
                    .flatMap(issue -> IVPair.convertValueSet(issue, domain.getValues(issue)).stream())
                    .collect(Collectors.toSet());
            TreeSet<IVPair> orderedMapping = new TreeSet<IVPair>(unorderedMapping);
            // List<String> stateNames = orderedMapping.stream().map(ivp ->
            // ivp.toString()).collect(Collectors.toList());
            // schema = new Schema.Builder().addColumnCategorical(IV_COL,
            // stateNames).build();
            // encoder = new
            // TransformProcess.Builder(schema).categoricalToOneHot(IV_COL).build();
            this.setMatrixArray(Nd4j.eye(this.size() - 1));
            long length = this.getMatrixArray().shape()[0];
            long[] shape = { length, 1l };
            this.setContainerVector(Nd4j.zeros(length).reshape(shape));
            this.addAll(orderedMapping);
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
            return this.containerVector.dup();
        }

        private void setContainerVector(INDArray containerVector) {
            this.containerVector = containerVector;
        }

        public INDArray getVector(Integer idx) {
            if (idx == 0) {
                return this.getContainerVector();
            }
            return this.getMatrixArray().getRow(idx);
        };
    }

    public static class BidMapping {
        // private Schema schema;
        // private TransformProcess encoder;

        // public BidMapping(Domain domain) {
        //     super();

        //     Set<String> issues = domain.getIssues();
        //     ArrayList<IVPair> bids = IVPair.getIVPairMapping(domain).getMapping();


        //     Builder schemaBuilder = new Schema.Builder();
        //     for (String iss : issues) {
        //         ValueSet valueSet = domain.getValues(iss);
        //         List<String> valueSetList = IVPair.convertValueSet(valueSet);
        //         schemaBuilder.addColumnCategorical(iss, valueSetList);
        //     }
        //     this.schema = schemaBuilder.build();
        //     org.datavec.api.transform.TransformProcess.Builder encoderBuilder = new TransformProcess.Builder(schema);
        //     for (String iss : issues) {
        //         String remove = domain.getValues(iss).iterator().next().toString();
        //         encoderBuilder.categoricalToOneHot(iss).removeColumns(iss + "[" + remove + "]");
        //     }
        //     this.encoder = encoderBuilder.build();
        //     this.encoder.transformRawStringsToInput(values);
            
        //     // RecordReader reader = new RecordMapper; 
        //     /*
        //                                                          * first line to skip and // comma seperated
        //                                                          */
        //     // reader.initialize(new );
        //     // RecordReader transformProcessRecordReader = new
        //     // TransformProcessRecordReader(reader, transformProcess);
        //     // tra
        //     this.addAll(orderedMapping);
        // }

        // public List<Bid> genetaBidspace(domain){
            
        // }

        // private List<List<IVPair>> recurseBidSpace(List<String> issueList,  Domain domain, Integer idx) {
        //     if (idx > issueList.size()) {
        //         List<List<IVPair>> collector = new ArrayList<List<IVPair>>();
        //         ArrayList<IVPair> endList = new ArrayList<IVPair>();
        //         String issue = issueList.get(idx);
        //         for (String val : IVPair.convertValueSet(domain.getValues(issue))) {
        //             endList.add(new IVPair(issue, val));
        //         }
        //         collector.add(endList);
        //         return collector;
        //     }
            

        //     List<List<IVPair>> collector = new ArrayList<List<IVPair>>();
        //     String issue = issueList.get(idx);
        //     for (String val1 : IVPair.convertValueSet(domain.getValues(issue))) {
        //         collector.addAll(recurseBidSpace(issueList, domain, idx+1));
        //     }
        //     return collector;
        // };
    }

}
