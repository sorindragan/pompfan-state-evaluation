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

public class ExperimentStateEvalCostumRunner extends CustomNegoRunner {
        /**
         *
         */
        private static final Random RANDOM = new Random();
        private final static ObjectMapper jacksonReader = new ObjectMapper();
        private final static List<String> setNumParticlesPerOpponent = Arrays.asList("10");
        private final static List<String> setSimulationTime = Arrays.asList("200");
        private final static List<String> setDataCollectionTime = Arrays.asList("0.05");
        private final static List<String> setK = Arrays.asList("2.0");
        private final static List<String> setA = Arrays.asList("0.5");
        private final static List<String> setMaxWidth = Arrays.asList("15");

        private final static List<String> setExplorer = Arrays.asList("TimeConcedingExplorationPolicy");
        private final static List<String> setWidener = Arrays.asList("ProgressiveWideningStrategy");
        private final static List<String> setBelief = Arrays.asList("BayesianParticleFilterBelief");
        
        private final static List<String> setEvaluator = Arrays.asList("Last2BidsProductUtilityEvaluator",
        "Last2BidsMeanUtilityEvaluator", "Last2BidsOneMinusDifferenceUtilityEvaluator",
        "Last2BidsMixtMeanUtilityEvaluator", "Last2BidsMixtProdUtilEvaluator",
        "Last2BidsMixtOneMinusDifferenceUtilEvaluator", "RandomEvaluator");
        
        private final static List<String> setComparer = Arrays.asList("JaccardBidDistance",
        "SDiceBidDistance",
        "IssueValueCountBidDistance");
        
        private final static List<String> setOpponents = Arrays.asList(
                        "classpath:geniusweb.exampleparties.anac2021.tripleagent.TripleAgent",
                        "classpath:geniusweb.exampleparties.anac2021.agentfo.AgentFO",
                        "classpath:geniusweb.exampleparties.anac2021.dicehaggler.TheDiceHaggler2021", 
                        "classpath:geniusweb.exampleparties.anac2021.gambleragent.GamblerAgent",
                        "classpath:geniusweb.exampleparties.anac2021.aorta.AortaBoa",
                        "classpath:geniusweb.opponents.BoulwareParty",
                        "classpath:geniusweb.opponents.ConcederParty",
                        "classpath:geniusweb.opponents.OwnUtilTFTAgent"
                        );

        public ExperimentStateEvalCostumRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory,
                        Reporter logger, long maxruntime, String settingRef, String name) throws IOException {
                super(settings, connectionfactory, logger, maxruntime, settingRef, name);
        }

        public static void main(String[] args) throws JsonMappingException, JsonProcessingException, IOException {
                JsonNode settingsJson = jacksonReader
                                .readTree(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));

                int cnt = 0;
                for (String distanceMeasure : setComparer) {
                        for (String stateEval : setEvaluator) {
                                for (String currOpponent : setOpponents) {
                                       cnt++;
                                        ObjectNode target = (ObjectNode) settingsJson.get("SAOPSettings")
                                                        .get("participants").get(0)
                                                        .get("TeamInfo").get("parties").get(0).get("party");

                                        Map<String, Object> parameters = new HashMap<String, Object>();
                                        Map<String, Object> config = new HashMap<String, Object>();
                                        Map<String, Object> confExtra = new HashMap<String, Object>();
                                        Map<String, Object> widener = new HashMap<String, Object>();

                                        parameters.put("numParticlesPerOpponent", setNumParticlesPerOpponent.get(0));
                                        parameters.put("simulationTime", setSimulationTime.get(0));
                                        parameters.put("dataCollectionTime", setDataCollectionTime.get(0));
                                        parameters.put("persistentstate", "59853b79-f3f8-4179-8b57-7b5b2e9eb2f7");
                                        parameters.put("config", config);

                                        config.put("confState", "HistoryState");
                                        config.put("confBelief", setBelief.get(0));
                                        config.put("confExplorer", setExplorer.get(0));
                                        config.put("confWidener", setWidener.get(0));
                                        config.put("confComparer", distanceMeasure);
                                        config.put("confEvaluator", stateEval);
                                        config.put("confExtra", confExtra);

                                        confExtra.put("widener", widener);

                                        widener.put("maxWidth", setMaxWidth.get(0));
                                        widener.put("k_a", setK.get(0));
                                        widener.put("k_b", setK.get(0));
                                        widener.put("a_a", setA.get(0));
                                        widener.put("a_b", setA.get(0));

                                        target.set("parameters",
                                                        jacksonReader.convertValue(parameters, JsonNode.class));

                                        ObjectNode opponent = (ObjectNode) settingsJson.get("SAOPSettings")
                                                        .get("participants").get(1)
                                                        .get("TeamInfo").get("parties").get(0).get("party");

                                        Map<String, Object> oppParameters = new HashMap<String, Object>();
                                        String ref = currOpponent;
                                        oppParameters.put("persistentstate", "59853b79-f3f8-4179-8b57-7b5b2e9eb11" + cnt % 9);
                                        opponent.put("partyref", ref);
                                        opponent.set("parameters", jacksonReader.convertValue(oppParameters, JsonNode.class));

                                        String settingRef = args[0];

                                        Reporter logger = new StdOutReporter();
                                        NegoSettings settings = jacksonReader.readValue(settingsJson.toString(),
                                                        NegoSettings.class);

                                        logger.log(Level.INFO, "Starting Session " + cnt);
                                        logger.log(Level.INFO, settingsJson.toString());

                                        FileWriter finalLogWriter = new FileWriter(
                                                        "eval/tournament_results_ExperimentStateEval.jsonl", true);
                                        String content = settings.toString().split("confEvaluator=")[1].split(",")[0]
                                                        .split("}")[0] + "," +
                                                        settings.toString().split("confComparer=")[1].split(",")[0]
                                                                        .split("}")[0]
                                                        + "," + "POMPFANAgent" + "," + 
                                                        currOpponent.split(".")[currOpponent.split(".").length-1] + "\n";

                                        finalLogWriter.write(content);
                                        finalLogWriter.close();

                                        NegoRunner runner = new ExperimentStateEvalCostumRunner(settings,
                                                        new ClassPathConnectionFactory(),
                                                        logger, 0, settingRef, "ExperimentStateEval");
                                        runner.run();
                                        // if (runner.isProperlyStopped()) {
                                                // do something
                                        // }

                                }
                        }

                }
                System.out.println("Experminet Done");
                System.exit(0);
        }

        public static ObjectMapper getJacksonreader() {
                return jacksonReader;
        }

}
