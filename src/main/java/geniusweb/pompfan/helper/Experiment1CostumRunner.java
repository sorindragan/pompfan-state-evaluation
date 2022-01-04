package geniusweb.pompfan.helper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import geniusweb.protocol.NegoSettings;
import geniusweb.protocol.partyconnection.ProtocolToPartyConnFactory;
import geniusweb.simplerunner.ClassPathConnectionFactory;
import geniusweb.simplerunner.NegoRunner;
import tudelft.utilities.logging.Reporter;

public class Experiment1CostumRunner extends CustomNegoRunner {
        /**
         *
         */
        private static final Random RANDOM = new Random();
        private final static ObjectMapper jacksonReader = new ObjectMapper();
        private final static List<String> setNumParticlesPerOpponent = IntStream.range(1, 2).boxed()
                        .map(String::valueOf).map(e -> e + "0").collect(Collectors.toList());
        private final static List<String> setSimulationTime = Arrays.asList("200");
        private final static List<String> setDataCollectionTime = Arrays.asList("0.1");
        private final static List<String> setK = Arrays.asList("2.0");
        private final static List<String> setA = Arrays.asList("0.5");
        private final static List<String> setMaxWidth = Arrays.asList("15");

        private final static List<String> setExplorer = Arrays.asList("TimeConcedingExplorationPolicy");
        private final static List<String> setWidener = Arrays.asList("ProgressiveWideningStrategy");
        private final static List<String> setBelief = Arrays.asList("BayesianParticleFilterBelief");
        
        private final static List<String> setEvaluator = Arrays.asList("Last2BidsProductUtilityEvaluator",
        "Last2BidsMeanUtilityEvaluator", "Last2BidsOneMinusDifferenceUtilityEvaluator",
        "Last2BidsMixtMeanUtilityEvaluator", "Last2BidsMixtProdUtilEvaluator",
        "Last2BidsMixtInverseDifferenceUtilEvaluator", "RandomEvaluator");
        
        private final static List<String> setComparer = Arrays.asList("UtilityBidDistance", "OppUtilityBidDistance",
        "BothUtilityBidDistance",  "JaccardBidDistance", "SDiceBidDistance",
        "ExactSameBidDistance", "IssueValueCountBidDistance", "RandomBidDistance");
        
        private final static List<String> setOpponents = Arrays.asList(
                        "classpath:geniusweb.opponents.OwnUtilTFTAgent",
                        "classpath:geniusweb.opponents.OppUtilTFTAgent",
                        // "classpath:geniusweb.opponents.AntagonisticAgent", 
                        // "classpath:geniusweb.opponents.SelfishAgent",
                        "classpath:geniusweb.exampleparties.boulware.Boulware",
                        // "classpath:geniusweb.exampleparties.hardliner.Hardliner",
                        // "classpath:geniusweb.exampleparties.timedependentparty.TimeDependentParty",
                        "classpath:geniusweb.exampleparties.randomparty.RandomParty",
                        "classpath:geniusweb.exampleparties.conceder.Conceder"
                        // "classpath:geniusweb.exampleparties.simpleboa.SimpleBoa"
                        );

        public Experiment1CostumRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory,
                        Reporter logger, long maxruntime, String settingRef, String name) throws IOException {
                super(settings, connectionfactory, logger, maxruntime, settingRef, name);
        }

        public static void main(String[] args) throws JsonMappingException, JsonProcessingException, IOException {
                JsonNode settingsJson = jacksonReader
                                .readTree(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));

                for (int i = 0; i < 336; i++) {
                        Integer cnt = i+1;
                        ObjectNode target = (ObjectNode) settingsJson.get("SAOPSettings").get("participants").get(0)
                                        .get("TeamInfo").get("parties").get(0).get("party");

                        Map<String, Object> parameters = new HashMap<String, Object>();
                        Map<String, Object> config = new HashMap<String, Object>();
                        Map<String, Object> confExtra = new HashMap<String, Object>();
                        Map<String, Object> widener = new HashMap<String, Object>();

                        parameters.put("numParticlesPerOpponent", setNumParticlesPerOpponent
                                        .get(RANDOM.nextInt(setNumParticlesPerOpponent.size())));
                        parameters.put("simulationTime",
                                        setSimulationTime.get(RANDOM.nextInt(setSimulationTime.size())));
                        parameters.put("dataCollectionTime",
                                        setDataCollectionTime.get(RANDOM.nextInt(setDataCollectionTime.size())));
                        parameters.put("persistentstate", "59853b79-f3f8-4179-8b57-7b5b2e9eb2f7");
                        parameters.put("config", config);

                        config.put("confComparer", setComparer.get(RANDOM.nextInt(setComparer.size())));
                        config.put("confBelief", setBelief.get(RANDOM.nextInt(setBelief.size())));
                        config.put("confEvaluator", setEvaluator.get(RANDOM.nextInt(setEvaluator.size())));
                        config.put("confState", "HistoryState");
                        config.put("confExplorer", setExplorer.get(RANDOM.nextInt(setExplorer.size())));
                        config.put("confWidener", setWidener.get(RANDOM.nextInt(setWidener.size())));
                        config.put("confExtra", confExtra);

                        confExtra.put("widener", widener);

                        widener.put("maxWidth", setMaxWidth.get(RANDOM.nextInt(setK.size())));
                        widener.put("k_a", setK.get(RANDOM.nextInt(setMaxWidth.size())));
                        widener.put("k_b", setK.get(RANDOM.nextInt(setMaxWidth.size())));
                        widener.put("a_a", setA.get(RANDOM.nextInt(setA.size())));
                        widener.put("a_b", setA.get(RANDOM.nextInt(setA.size())));

                        target.set("parameters", jacksonReader.convertValue(parameters, JsonNode.class));

                        ObjectNode opponent = (ObjectNode) settingsJson.get("SAOPSettings").get("participants").get(1)
                                        .get("TeamInfo").get("parties").get(0).get("party");

                        Map<String, Object> oppParameters = new HashMap<String, Object>();
                        String ref = setOpponents.get(RANDOM.nextInt(setOpponents.size()));
                        oppParameters.put("persistentstate", "59853b79-f3f8-4179-8b57-7b5b2e9eb111");
                        opponent.put("partyref", ref);
                        opponent.set("parameters", jacksonReader.convertValue(oppParameters, JsonNode.class));

                        String settingRef = args[0];

                        Reporter logger = new StdOutReporter();
                        NegoSettings settings = jacksonReader.readValue(settingsJson.toString(), NegoSettings.class);

                        logger.log(Level.INFO, "Starting Session " + cnt);
                        logger.log(Level.INFO, settingsJson.toString());
                        
                        FileWriter finalLogWriter = new FileWriter("eval/tournament_results_Experiment1A.jsonl", true);
                        String content = settings.toString().split("confEvaluator=")[1].split(",")[0].split("}")[0] + "," +
                                         settings.toString().split("confComparer=")[1].split(",")[0].split("}")[0] + ",";
                        // TODO: get the utility of the opponent somehow
                        // TODO: see if can get other metrics
                        finalLogWriter.write(content);
                        finalLogWriter.close();


                        NegoRunner runner = new Experiment1CostumRunner(settings, new ClassPathConnectionFactory(),
                                        logger, 0, settingRef, "Experiment1A");
                        runner.run();
                        // if (runner.isProperlyStopped()) {
                        //         // do something        
                        // }
                }

                System.exit(0);
        }

        public static ObjectMapper getJacksonreader() {
                return jacksonReader;
        }

}
