package geniusweb.pompfan.helper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import geniusweb.events.ProtocolEvent;
import geniusweb.protocol.CurrentNegoState;
import geniusweb.protocol.NegoSettings;
import geniusweb.protocol.NegoState;
import geniusweb.protocol.partyconnection.ProtocolToPartyConnFactory;
import geniusweb.protocol.session.SessionResult;
import geniusweb.protocol.session.SessionState;
import geniusweb.protocol.tournament.allpermutations.AllPermutationsState;
import geniusweb.simplerunner.ClassPathConnectionFactory;
import geniusweb.simplerunner.NegoRunner;
import tudelft.utilities.logging.Reporter;

public class CustomNegoRunner extends NegoRunner {
    private final static ObjectMapper jacksonReader = new ObjectMapper();
    private final static ObjectWriter jacksonWriter = jacksonReader.writerWithDefaultPrettyPrinter();
    private final static ObjectWriter jacksonWriterCompact = jacksonReader.writer();
    private String timestampString;
    private SimpleDateFormat tFormatter = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
    private ResultsWriter intermediateWriter;
    private Integer sessNum = 0;
    private Date startTimeStamp;

    public CustomNegoRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory, Reporter logger,
            long maxruntime, String settingRef, String name) throws IOException {
        super(settings, connectionfactory, logger, maxruntime);
        this.startTimeStamp = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
        this.timestampString = formatter.format(this.startTimeStamp);
        FileWriter collectorFile = new FileWriter("eval/tournament_results_" + name + ".jsonl", true);
        this.intermediateWriter = new ResultsWriter(this.getProtocol().getState(),
                this.tFormatter.format(startTimeStamp), settingRef, collectorFile);
        this.getProtocol().addListener(ev -> this.processSessionEnd(ev));
        // TODO: Add cleanup with listener;
    }

    @Override
    protected void logFinal(Level level, NegoState state) {
        FileWriter resultsJsonFileWriter;
        try {
            // log.log(level, "protocol ended normally: " + jacksonWriterCompact.writeValueAsString(state));
            Date now = new Date();
            String logFileName = "none";
            ResultsLogger finalResults = new ResultsLogger(state, this.tFormatter.format(now));
            if (state instanceof AllPermutationsState) {
                logFileName = "log_tournament" + "_" + this.timestampString;
            } else if (state instanceof SessionState) {
                logFileName = "log_session" + "_" + this.timestampString;
            }
            resultsJsonFileWriter = new FileWriter("logs/" + logFileName + ".json");
            resultsJsonFileWriter.write(jacksonWriter.writeValueAsString(finalResults));
            resultsJsonFileWriter.close();

            // System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Called when the session completed.
     * 
     * @param data the ProtocolEvent data,
     */
    private void processSessionEnd(ProtocolEvent data) {
        if (data instanceof CurrentNegoState) {
            NegoState state = ((CurrentNegoState) data).getState();
            if (state instanceof AllPermutationsState) {
                this.sessNum++;
                AllPermutationsState tournamentState = (AllPermutationsState) state;
                int idxLastFinishedSession = tournamentState.getResults().size() - 1;
                SessionResult sessionState = (SessionResult) tournamentState.getResults().get(idxLastFinishedSession);
                Date sessionTimestamp = new Date();
                String sessionTime = this.tFormatter.format(sessionTimestamp);
                try {
                    this.intermediateWriter.writeSession(this.sessNum, sessionState, sessionTime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        List<NegoSettings> settings = Arrays.asList(args).stream().map(arg -> {
            try {
                String settingString = new String(Files.readAllBytes(Paths.get(arg)), StandardCharsets.UTF_8);
                return jacksonReader.readValue(settingString, NegoSettings.class);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        Integer cnt = 0;
        Date runStartTime = new Date();
        for (NegoSettings negoSettings : settings) {
            String settingRef = args[cnt];
            String name = Paths.get(settingRef).getParent().getFileName().toString();
            cnt++;
            Reporter logger = new StdOutReporter();
            logger.log(Level.INFO, "Starting Tournament " + cnt);
            NegoRunner runner = new CustomNegoRunner(negoSettings, new ClassPathConnectionFactory(), logger, 0,
                    settingRef, name);
            runner.run();
        }

        System.exit(0);
    }
}

class StdOutReporter implements Reporter {

    @Override
    public void log(Level arg0, String arg1) {
        System.out.println(arg0 + ":" + arg1);
    }

    @Override
    public void log(Level arg0, String arg1, Throwable arg2) {
        System.out.println(arg0 + ">" + arg1);
    }

}
