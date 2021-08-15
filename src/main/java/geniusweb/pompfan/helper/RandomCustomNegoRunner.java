package geniusweb.pompfan.helper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class RandomCustomNegoRunner extends CustomNegoRunner {
    /**
     *
     */
    private static final Random RANDOM = new Random();
    private final static ObjectMapper jacksonReader = new ObjectMapper();
    private final static List<String> setNumParticlesPerOpponent = IntStream.range(1, 15).boxed().map(String::valueOf)
            .map(e -> e + "0").collect(Collectors.toList());
    private final static List<String> setSimulationTime = IntStream.range(1, 10).boxed().map(String::valueOf)
            .map(e -> e + "00").collect(Collectors.toList());
    private final static List<String> setK = RANDOM.doubles(10, 2.0, 5.0).mapToObj(String::valueOf)
            .map(BigDecimal::new).map(e -> e.setScale(0, RoundingMode.DOWN)).map(String::valueOf).map(e -> e + ".0")
            .collect(Collectors.toList());
    private final static List<String> setA = RANDOM.doubles(10, 0.1, 1.0).mapToObj(String::valueOf)
            .map(BigDecimal::new).map(e -> e.setScale(1, RoundingMode.HALF_UP)).map(String::valueOf)
            .collect(Collectors.toList());
    private final static List<String> setMaxWidth = RANDOM.ints(10, 2, 15).mapToObj(String::valueOf)
            .map(BigDecimal::new).map(e -> e.setScale(1, RoundingMode.HALF_UP)).map(String::valueOf)
            .collect(Collectors.toList());

//     private final static List<String> setComparer = Arrays.asList("UtilityBidDistance", "JaccardBidDistance", "HammingBidDistance");
    private final static List<String> setComparer = Arrays.asList("JaccardBidDistance", "HammingBidDistance");
    private final static List<String> setBelief = Arrays.asList("ParticleFilterWithAcceptBelief", "ParticleFilterBelief");
    private final static List<String> setEvaluator = Arrays.asList("Last2BidsProductUtilityEvaluator", "Last2BidsMeanUtilityEvaluator", "RandomEvaluator");
    //     private final static List<String> setExplorer = Arrays.asList("HighSelfEsteemOwnExplorationPolicy", "TimeConcedingExplorationPolicy", "RandomOwnExplorerPolicy");
    private final static List<String> setExplorer = Arrays.asList("TimeConcedingExplorationPolicy", "RandomOwnExplorerPolicy");
//     private final static List<String> setWidener = Arrays.asList("ProgressiveWideningStrategy", "MaxWidthWideningStrategy");
    private final static List<String> setWidener = Arrays.asList("MaxWidthWideningStrategy");

    public RandomCustomNegoRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory, Reporter logger,
            long maxruntime, String settingRef, String name) throws IOException {
        super(settings, connectionfactory, logger, maxruntime, settingRef, name);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws JsonMappingException, JsonProcessingException, IOException {
        JsonNode settingsJson = jacksonReader
                .readTree(new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8));

        Integer cnt = 0;
        for (int i = 0; i < 20; i++) {
            cnt++;
            ObjectNode target = (ObjectNode) settingsJson.get("AllPermutationsSettings").get("teams").get(0).get("Team")
                    .get(0);

            Map<String, Object> parameters = new HashMap<String, Object>();
            Map<String, Object> config = new HashMap<String, Object>();
            Map<String, Object> confExtra = new HashMap<String, Object>();
            Map<String, Object> widener = new HashMap<String, Object>();

            parameters.put("numParticlesPerOpponent",
                    setNumParticlesPerOpponent.get(RANDOM.nextInt(setNumParticlesPerOpponent.size())));
            parameters.put("simulationTime", setSimulationTime.get(RANDOM.nextInt(setSimulationTime.size())));
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
            widener.put("k_b", "1.0");
            widener.put("a_a", setA.get(RANDOM.nextInt(setA.size())));
            widener.put("a_b", setA.get(RANDOM.nextInt(setA.size())));

            target.set("parameters", jacksonReader.convertValue(parameters, JsonNode.class));

            String settingRef = args[0];

            Reporter logger = new StdOutReporter();
            NegoSettings settings = jacksonReader.readValue(settingsJson.toString(), NegoSettings.class);

            logger.log(Level.INFO, "Starting Tournament " + cnt);
            logger.log(Level.INFO, settingsJson.toString());
            NegoRunner runner = new RandomCustomNegoRunner(settings, new ClassPathConnectionFactory(), logger, 0,
                    settingRef, "random");
            runner.run();
        }

        System.exit(0);
    }

    public static ObjectMapper getJacksonreader() {
        return jacksonReader;
    }

}
