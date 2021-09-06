package geniusweb.pompfan.opponentModels;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.eigen.Eigen;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.guava.collect.HashBasedTable;
import org.nd4j.shade.guava.collect.Table;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.PartyId;
import geniusweb.issuevalue.Bid;
import geniusweb.issuevalue.DiscreteValue;
import geniusweb.issuevalue.Domain;
import geniusweb.issuevalue.Value;
import geniusweb.issuevalue.ValueSet;
import geniusweb.opponentmodel.OpponentModel;
import geniusweb.profile.utilityspace.DiscreteValueSetUtilities;
import geniusweb.profile.utilityspace.ValueSetUtilities;
import geniusweb.progress.Progress;

public class AHPWeightedOpponentModel extends AbstractOpponentModel {

    private Map<String, BigDecimal> issueChanges;
    private Map<String, Double> iChanges;
    private Map<String, Table<DiscreteValue, DiscreteValue, Double>> fCompMatrix;
    private Map<String, Table<DiscreteValue, DiscreteValue, Double>> base;
    private INDArray issueMathCompMatrix;
    private INDArray issueMathCompEigenvectors;
    // private Map<String, WinsOver> lWins;
    private Map<String, INDArray> mathCompMatrix = new HashMap<>();
    private Map<String, INDArray> mathCompEigenvectors = new HashMap<>();
    private Map<String, Integer> numValues;
    private Map<String, List<DiscreteValue>> mapping;
    private List<EigenValues> iComparisons;
    private List<EigenValues> vComparisons;
    private EigenValues issueWeightsVector;
    private Map<String, EigenValues> valueWeightsVector;
    private PartyId me;

    public AHPWeightedOpponentModel(Domain domain, List<Action> history, PartyId me) {
        super(domain, history);
        Set<String> allIssues = this.getDomain().getIssues();
        this.me = me;
        this.issueChanges = allIssues.stream().collect(Collectors.toMap(e -> e, e -> BigDecimal.ONE));

        this.numValues = allIssues.stream()
                .collect(Collectors.toMap(e -> e, e -> this.getDomain().getValues(e).size().intValue()));

        this.mapping = allIssues.stream().collect(Collectors.toMap(e -> e, e -> getValueMap(e)));
        this.fCompMatrix = allIssues.stream().collect(Collectors.toMap(e -> e, e -> HashBasedTable.create()));
        this.base = allIssues.stream().collect(Collectors.toMap(e -> e, e -> HashBasedTable.create()));
        this.iChanges = new HashMap<>();
        for (String issue1 : allIssues) {
            this.iChanges.put(issue1, 1.0);
        }

        for (String issue : allIssues) {
            Table<DiscreteValue, DiscreteValue, Double> table = this.fCompMatrix.get(issue);
            ValueSet allValues = this.getDomain().getValues(issue);
            for (Value value1 : allValues) {
                DiscreteValue v1 = (DiscreteValue) value1;
                for (Value value2 : allValues) {
                    DiscreteValue v2 = (DiscreteValue) value2;
                    table.put(v1, v2, 1.0);
                }
            }
        }
        for (String issue : allIssues) {
            Table<DiscreteValue, DiscreteValue, Double> table = this.base.get(issue);
            ValueSet allValues = this.getDomain().getValues(issue);
            for (Value value1 : allValues) {
                DiscreteValue v1 = (DiscreteValue) value1;
                for (Value value2 : allValues) {
                    DiscreteValue v2 = (DiscreteValue) value2;
                    table.put(v1, v2, 1.0);
                }
            }
        }
        // this.base = this.fCompMatrix;
        // this.sCompMatrix = this.getDomain().getIssues().stream()
        // .collect(Collectors.toMap(e -> e, e -> HashBasedTable.create()));
        // this.fCompMatrix = this.getDomain().getIssues().stream().map(
        // e -> new AbstractMap.SimpleEntry<String, INDArray>(e,
        // Nd4j.ones(numValues.get(e), numValues.get(e))))
        // .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        // this.sCompMatrix = this.getDomain().getIssues().stream().map(
        // e -> new AbstractMap.SimpleEntry<String, INDArray>(e,
        // Nd4j.ones(numValues.get(e), numValues.get(e))))
        // .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        if (history != null && history.size() > 4) {
            this.initializeModel(history);
        }

    }

    public List<EigenValues> getiComparisons() {
        return iComparisons;
    }

    public void setiComparisons(List<EigenValues> iComparisons) {
        this.iComparisons = iComparisons;
    }

    private AHPWeightedOpponentModel initializeModel(List<Action> history) {
        List<Action> myActions = history.stream().filter(action -> action.getActor().equals(this.me))
                .collect(Collectors.toList()); // Us
        List<Action> oppActions = history.stream().filter(action -> !action.getActor().equals(this.me))
                .collect(Collectors.toList()); // Opponent
        Integer numPairs = Math.min(myActions.size(), oppActions.size());
        Integer cnt = 0;
        Bid lastBid = ((ActionWithBid) oppActions.get(0)).getBid();
        HashMap<String, List<WinsOver>> lWins = new HashMap<String, List<WinsOver>>();
        for (int i = 0; i < numPairs; i++) {
            Bid firstBid = ((ActionWithBid) myActions.get(i)).getBid();
            Bid secondBid = ((ActionWithBid) oppActions.get(i)).getBid();
            for (String nextIssue : this.getDomain().getIssues()) {
                List<WinsOver> wins = lWins.getOrDefault(nextIssue, new ArrayList<WinsOver>());
                DiscreteValue firstVal = (DiscreteValue) firstBid.getValue(nextIssue);
                DiscreteValue secondVal = (DiscreteValue) secondBid.getValue(nextIssue);

                Value lastOpponentValue = lastBid.getValue(nextIssue);
                if (secondVal.equals(lastOpponentValue)) {
                    this.iChanges = incrementIssueTable(nextIssue, this.iChanges);
                }

                if (!firstVal.equals(secondVal)) {
                    wins.add(new WinsOver(secondVal, firstVal));
                } else {
                    wins.add(null);
                }
                lWins.put(nextIssue, wins);
            }

            lastBid = secondBid;

        }
        this.fCompMatrix = this.incrementValueTable(lWins);
        this.generateValueMatrices(this.fCompMatrix);
        this.generateIssueMatrices(this.compareConsecutiveChanges(this.iChanges));
        this.computeEigenvectors(this.issueMathCompMatrix, this.mathCompMatrix);
        this.updateWeights(this.issueWeightsVector, this.valueWeightsVector);
        return this;
    }

    private Map<String, Table<DiscreteValue, DiscreteValue, Double>> incrementValueTable(
            HashMap<String, List<WinsOver>> lWins) {
        Map<String, Table<DiscreteValue, DiscreteValue, Double>> result = new HashMap<>(this.base);

        try {
            for (String issueString : this.getDomain().getIssues()) {
                Table<DiscreteValue, DiscreteValue, Double> table = HashBasedTable.create();
                for (WinsOver win : lWins.get(issueString)) {
                    if (win == null) {
                        continue;
                    }
                    DiscreteValue winner = win.getWinner();
                    DiscreteValue loser = win.getLoser();
                    @Nullable
                    Double currCnt = table.get(winner, loser);
                    Double nxtCnt = currCnt == null ? 1.0 : currCnt + 1.0;
                    table.put(winner, loser, nxtCnt);
                    table.put(loser, winner, 1 / nxtCnt);
                }

                result.put(issueString, table);
            }
            for (String issueString : this.getDomain().getIssues()) {
                Table<DiscreteValue, DiscreteValue, Double> table = result.get(issueString);
                for (Value v1 : this.getDomain().getValues(issueString)) {
                    for (Value v2 : this.getDomain().getValues(issueString)) {
                        Double currCnt = table.get(v1, v2);
                        if (currCnt == null) {
                            table.put((DiscreteValue) v1, (DiscreteValue) v2, 1.0);
                        }
                    }
                }
                result.put(issueString, table);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
        // return null;
    }

    private void updateWeights(EigenValues issueWeightsVector2, Map<String, EigenValues> valueWeightsVector2) {
        int cnt = 0;
        Set<String> allIssues = this.getDomain().getIssues();
        HashMap<String, ValueSetUtilities> issueUtilities = new HashMap<String, ValueSetUtilities>();
        HashMap<String, BigDecimal> issueWeights = new HashMap<String, BigDecimal>();
        for (String issue : allIssues) {
            Double weight = issueWeightsVector2.getNormalizedEigenvector().toDoubleVector()[cnt];
            issueWeights.put(issue, new BigDecimal(weight));
            cnt++;

            int valCnt = 0;
            Map<DiscreteValue, BigDecimal> issueValues = new HashMap<>();
            Set<DiscreteValue> allValues = this.fCompMatrix.get(issue).columnKeySet();
            double[] allWeights = this.valueWeightsVector.get(issue).getNormalizedEigenvector().toDoubleVector();

            for (DiscreteValue v : allValues) {
                issueValues.put(v, new BigDecimal(allWeights[valCnt]));
                valCnt++;
            }
            issueUtilities.put(issue, new DiscreteValueSetUtilities(issueValues));
        }

        this.setUtilities(issueUtilities);
        this.setIssueWeights(issueWeights);

    }

    private void generateIssueMatrices(Table<String, String, Double> compareConsecutiveChanges) {
        Table<String, String, Double> currentVals = compareConsecutiveChanges;
        double[] vals = currentVals.values().stream().mapToDouble(Double::doubleValue).toArray();
        int[] shape = new int[] { currentVals.rowKeySet().size(), currentVals.columnKeySet().size() };
        INDArray matrixForm = Nd4j.create(vals).reshape(shape);
        // System.out.println(matrixForm);
        this.issueMathCompMatrix = matrixForm;
        // System.out.println(this.issueMathCompMatrix);
    }

    private Table<String, String, Double> compareConsecutiveChanges(Map<String, Double> iChanges2) {
        Set<String> allIssues = this.getDomain().getIssues();
        Table<String, String, Double> result = HashBasedTable.create();
        for (String issue1 : allIssues) {
            for (String issue2 : allIssues) {
                double ratio = iChanges2.get(issue1) / iChanges2.get(issue2);
                result.put(issue1, issue2, ratio);
            }
        }
        return result;
    }

    private void computeEigenvectors(INDArray issueMathCompMatrix2, Map<String, INDArray> mathCompMatrix2) {
        Set<String> allIssues = this.getDomain().getIssues();
        // INDArray sums = issueMathCompMatrix2.sum(0);
        // System.out.println(sums);
        // INDArray normalizedMatrix = issueMathCompMatrix2.divRowVector(sums);
        // INDArray squared = issueMathCompMatrix2.mul(issueMathCompMatrix2);
        // System.out.println("===============T===============");
        this.issueWeightsVector = new EigenValues(issueMathCompMatrix2, false);
        // System.out.println(issueMathCompMatrix2);
        // System.out.println("----");
        // System.out.println(squared);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.eigenValues);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.normalizedMatrix);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.maxEigenvector);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.normalizedEigenvector);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.CI);
        // System.out.println("----");
        // System.out.println(this.issueWeightsVector.CR);
        // System.out.println("===============F===============");
        this.valueWeightsVector = new HashMap<String, EigenValues>();
        for (String issueString : allIssues) {
            System.out.println("===============T===============");
            System.out.println(issueString);
            INDArray compMatrix = mathCompMatrix2.get(issueString);
            System.out.println(compMatrix);
            System.out.println("----");
            EigenValues weightContainer = new EigenValues(compMatrix, false);
            System.out.println(weightContainer);
            System.out.println("----");
            System.out.println(weightContainer.eigenValues);
            System.out.println("----");
            System.out.println(weightContainer.normalizedMatrix);
            System.out.println("----");
            System.out.println(weightContainer.maxEigenvector);
            System.out.println("----");
            System.out.println(weightContainer.normalizedEigenvector);
            System.out.println("----");
            System.out.println(weightContainer.CI);
            System.out.println("----");
            System.out.println(weightContainer.CR);
            this.valueWeightsVector.put(issueString, weightContainer);
        }

    }

    private void generateValueMatrices(Map<String, Table<DiscreteValue, DiscreteValue, Double>> fCompMatrix2) {

        // System.out.println(iChanges2);
        // System.out.println(fCompMatrix2);
        double[] vals;
        int[] shape;
        INDArray matrixForm;
        for (String issueString : this.getDomain().getIssues()) {
            Table<DiscreteValue, DiscreteValue, Double> vCurrentVals = fCompMatrix2.get(issueString);
            vals = vCurrentVals.values().stream().mapToDouble(Double::doubleValue).toArray();
            shape = new int[] { vCurrentVals.rowKeySet().size(), vCurrentVals.columnKeySet().size() };
            matrixForm = Nd4j.create(vals).reshape(shape);
            // System.out.println(issueString);
            // System.out.println(matrixForm);
            this.mathCompMatrix.put(issueString, matrixForm);
            // System.out.println(this.mathCompMatrix.get(issueString));
            // System.out.println("stop");
        }
    }

    private INDArray getAHPMatrix(INDArray matrixForm) {
        INDArray currMatrix = matrixForm;
        INDArray upperTriangular = Nd4j.getBlasWrapper().lapack().getUFactor(currMatrix);
        INDArray lowerTriangular = Nd4j.getBlasWrapper().lapack()
                .getLFactor(Nd4j.create(new double[] { 1.0 }).div(currMatrix));
        INDArray ones = Nd4j.eye(upperTriangular.shape()[0]);
        INDArray container = Nd4j.zerosLike(upperTriangular);
        INDArray sumTri = container.add(upperTriangular).add(lowerTriangular);
        INDArray matrix = sumTri.sub(sumTri.mul(ones)).add(ones);
        return matrix;
    }

    private Map<String, Double> incrementIssueTable(String nextIssue, Map<String, Double> mapOfConsecutiveChanges) {
        Double consecutiveChanges = mapOfConsecutiveChanges.get(nextIssue);
        Double currCnt = consecutiveChanges == null ? 1.0 : consecutiveChanges + 1.0;
        mapOfConsecutiveChanges.put(nextIssue, currCnt);
        return mapOfConsecutiveChanges;
    }

    private Map<String, Table<DiscreteValue, DiscreteValue, Double>> incrementValueTable(
            HashMap<String, WinsOver> lWinsWinsOver,
            Map<String, Table<DiscreteValue, DiscreteValue, Double>> fCompMatrix2) {

        Map<String, Table<DiscreteValue, DiscreteValue, Double>> result = new HashMap<>();

        for (String issueString : this.getDomain().getIssues()) {
            try {
                Table<DiscreteValue, DiscreteValue, Double> table = HashBasedTable
                        .create(fCompMatrix2.get(issueString));
                WinsOver win = lWinsWinsOver.get(issueString);
                if (win == null) {
                    result.put(issueString, table);
                    continue;
                }

                DiscreteValue winner = win.getWinner();
                DiscreteValue loser = win.getLoser();
                @Nullable
                Double currCnt = table.get(winner, loser);
                Double nxtCnt = currCnt == null ? 1.0 : currCnt + 1.0;
                table.put(winner, loser, nxtCnt);
                table.put(loser, winner, 1 / nxtCnt);
                result.put(issueString, table);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private Table<DiscreteValue, DiscreteValue, Double> incrementValueTable(String nextIssue,
            Table<DiscreteValue, DiscreteValue, Double> table, DiscreteValue firstVal, DiscreteValue secondVal) {
        @Nullable
        Double currCnt = table.get(secondVal, firstVal) == null ? 1.0 : table.get(secondVal, firstVal) + 1.0;
        table.put(secondVal, firstVal, currCnt);
        table.put(firstVal, secondVal, 1 / currCnt);
        // table.put(secondVal, firstVal, 1 / currCnt);
        return table;
    }

    private List<DiscreteValue> getValueMap(String e) {
        return StreamSupport.stream(this.getDomain().getValues(e).spliterator(), false).map(v -> (DiscreteValue) v)
                .collect(Collectors.toList());

    }

    @Override
    public BigDecimal getUtility(Bid bid) {
        return this.getWeights().keySet().stream().map(iss -> this.util(iss, bid.getValue(iss))).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    @Override
    public OpponentModel with(Domain domain, Bid resBid) {
        return this;
    }

    @Override
    public OpponentModel with(Action action, Progress progress) {
        this.getHistory().add(action);
        return this.initializeModel(this.getHistory());
    }

    public Map<String, BigDecimal> getIssueChanges() {
        return issueChanges;
    }

    public void setIssueChanges(Map<String, BigDecimal> issueChanges) {
        this.issueChanges = issueChanges;
    }

    public Map<String, Integer> getNumValues() {
        return numValues;
    }

    public void setNumValues(Map<String, Integer> numValues) {
        this.numValues = numValues;
    }

    public Map<String, List<DiscreteValue>> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, List<DiscreteValue>> mapping) {
        this.mapping = mapping;
    }

    /**
     * Pair
     */
    public class EigenValues {

        private double maxValue;
        private int maxIndex;
        private INDArray maxEigenvector;
        private INDArray normalizedEigenvector;
        private Double[] RCI = new Double[] { 0.0, 0.0, 0.58, 0.9, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.51, 1.48, 1.56,
                1.57, 1.59 };
        private int nAlternatives;
        private double CI;
        private double CR;
        private INDArray normalizedMatrix;
        private INDArray eigenValues;

        public EigenValues(INDArray symmetricMatrix, Boolean flag) {
            this.normalizedMatrix = symmetricMatrix.dup();
            INDArray normalizedMatrix1 = this.normalizedMatrix;
            INDArray normalizedMatrix2 = this.normalizedMatrix.divRowVector(this.normalizedMatrix.sum(0));
            normalizedMatrix2 = normalizedMatrix2.mul(normalizedMatrix2);

            if (flag) {
                this.eigenValues = Eigen.symmetricGeneralizedEigenvalues(normalizedMatrix1, true);
                this.nAlternatives = symmetricMatrix.columns();
                this.maxValue = eigenValues.max(0).toDoubleVector()[0];
                this.maxIndex = eigenValues.argMax(0).toIntVector()[0];
                this.CI = (this.maxValue - this.nAlternatives) / (this.nAlternatives - 1);
                this.CR = this.CI / this.RCI[this.nAlternatives - 1];
                this.normalizedMatrix = normalizedMatrix1;
            } else {
                this.eigenValues = Eigen.symmetricGeneralizedEigenvalues(normalizedMatrix2, true);
                this.nAlternatives = symmetricMatrix.columns();
                this.maxValue = eigenValues.max(0).toDoubleVector()[0];
                this.maxIndex = eigenValues.argMax(0).toIntVector()[0];
                this.CI = (this.maxValue - this.nAlternatives) / (this.nAlternatives - 1);
                this.CR = this.CI / this.RCI[this.nAlternatives - 1];
                this.normalizedMatrix = normalizedMatrix2;
            }
            // System.out.println("======================================================");
            // System.out.println(symmetricMatrix);
            // System.out.println("-------------------");
            // System.out.println(eigenValues);
            // System.out.println("-------------------");
            // System.out.println(normalizedMatrix);
            // System.out.println("-------------------");
            this.maxEigenvector = normalizedMatrix.getColumn(maxIndex);
            // System.out.println(maxEigenvector);
            // System.out.println("-------------------");
            this.normalizedEigenvector = maxEigenvector.div(maxEigenvector.sum(0));
            // System.out.println(normalizedEigenvector);
            // System.out.println("-------------------");
        }

        public double getMaxValue() {
            return maxValue;
        }

        public void setMaxValue(double maxValue) {
            this.maxValue = maxValue;
        }

        public int getMaxIndex() {
            return maxIndex;
        }

        public void setMaxIndex(int maxIndex) {
            this.maxIndex = maxIndex;
        }

        public INDArray getMaxEigenvector() {
            return maxEigenvector;
        }

        public void setMaxEigenvector(INDArray maxEigenvector) {
            this.maxEigenvector = maxEigenvector;
        }

        public INDArray getNormalizedEigenvector() {
            return normalizedEigenvector;
        }

        public void setNormalizedEigenvector(INDArray normalizedEigenvector) {
            this.normalizedEigenvector = normalizedEigenvector;
        }

        public int getnAlternatives() {
            return nAlternatives;
        }

        public void setnAlternatives(int nAlternatives) {
            this.nAlternatives = nAlternatives;
        }

        public double getCR() {
            return CR;
        }

        public void setCR(double cR) {
            CR = cR;
        }

        @Override
        public String toString() {
            return this.getNormalizedEigenvector().toString();
        }

    }

    /**
     * WinsOver
     */
    public class WinsOver {

        private Value winner;
        private Value loser;

        public WinsOver(Value winner, Value loser) {
            this.setWinner(winner);
            this.setLoser(loser);
        }

        public DiscreteValue getWinner() {
            return (DiscreteValue) winner;
        }

        public void setWinner(Value winner) {
            this.winner = winner;
        }

        public DiscreteValue getLoser() {
            return (DiscreteValue) loser;
        }

        public void setLoser(Value loser) {
            this.loser = loser;
        }

    }
}
