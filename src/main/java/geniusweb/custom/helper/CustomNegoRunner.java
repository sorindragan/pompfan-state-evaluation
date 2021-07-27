package geniusweb.custom.helper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import geniusweb.protocol.NegoSettings;
import geniusweb.protocol.NegoState;
import geniusweb.protocol.partyconnection.ProtocolToPartyConnFactory;
import geniusweb.protocol.session.SessionState;
import geniusweb.protocol.tournament.allpermutations.AllPermutationsState;
import geniusweb.simplerunner.ClassPathConnectionFactory;
import geniusweb.simplerunner.NegoRunner;
import tudelft.utilities.logging.Reporter;

public class CustomNegoRunner extends NegoRunner {
    private final static ObjectMapper jacksonReader = new ObjectMapper();
    private final static ObjectWriter jacksonWriter = jacksonReader.writerWithDefaultPrettyPrinter();
    private final static ObjectWriter jacksonWriterCompact = jacksonReader.writer();

    public CustomNegoRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory, Reporter logger,
            long maxruntime) {
        super(settings, connectionfactory, logger, maxruntime);
        // this.getProtocol().addListener(l);
    }

    @Override
    protected void logFinal(Level level, NegoState state) {
        FileWriter resultsJsonFileWriter;
        try {
            log.log(level, "protocol ended normally: " + jacksonWriterCompact.writeValueAsString(state));
            Date now = new Date();
            String timestamp = new SimpleDateFormat("dd_MM_yyyy_HH_mm").format(now);
            String temporalState = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy").format(now);
            String logFileName = "none";
            ResultsLogger finalResults = new ResultsLogger(state, temporalState);
            if (state instanceof AllPermutationsState) {
                logFileName = "log_tournament" + "_" + timestamp;
                FileWriter newestResultsJsonFileWriter = new FileWriter("logs/log_tournament_newest.json");
                newestResultsJsonFileWriter.write(jacksonWriter.writeValueAsString(finalResults));
                newestResultsJsonFileWriter.close();
            } else if (state instanceof SessionState) {
                logFileName = "log_session";
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

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        String serialized = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        NegoSettings settings = jacksonReader.readValue(serialized, NegoSettings.class);

        NegoRunner runner = new CustomNegoRunner(settings, new ClassPathConnectionFactory(), new StdOutReporter(), 0);
        runner.run();
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
