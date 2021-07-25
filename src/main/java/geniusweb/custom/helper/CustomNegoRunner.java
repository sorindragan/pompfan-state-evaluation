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

import geniusweb.protocol.NegoSettings;
import geniusweb.protocol.NegoState;
import geniusweb.protocol.partyconnection.ProtocolToPartyConnFactory;
import geniusweb.protocol.tournament.allpermutations.AllPermutationsState;
import geniusweb.simplerunner.ClassPathConnectionFactory;
import geniusweb.simplerunner.NegoRunner;
import tudelft.utilities.logging.Reporter;

public class CustomNegoRunner extends NegoRunner {
    private final static ObjectMapper jackson = new ObjectMapper();
    // private final NegoProtocol protocol;
    // private NegoSettings settings;
    // private ProtocolToPartyConnFactory connectionfactory;
    // private Reporter log;
    // private long maxruntime;

    public CustomNegoRunner(NegoSettings settings, ProtocolToPartyConnFactory connectionfactory, Reporter logger,
            long maxruntime) {
        super(settings, connectionfactory, logger, maxruntime);
        // if (settings == null || connectionfactory == null) {
        // throw new NullPointerException("Arguments must be not null");
        // }
        // this.settings = settings;
        // this.log = logger;
        // this.protocol = settings.getProtocol(log);
        // this.connectionfactory = connectionfactory;
        // this.maxruntime = maxruntime;
    }

    @Override
    protected void logFinal(Level level, NegoState state) {
        FileWriter fullTreeFileWriter;
        try {
            log.log(level, "protocol ended normally: " + jackson.writeValueAsString(state));
            String date = new SimpleDateFormat("dd_MM_yyyy_hh_mm").format(new Date());
            String logFileName = state instanceof AllPermutationsState ? "log_tournament" : "log_session";
            fullTreeFileWriter = new FileWriter("logs/" + logFileName + "_" + date + ".json");
            ResultsLogger finalResults = new ResultsLogger(state);
            fullTreeFileWriter.write(jackson.writeValueAsString(finalResults));
            fullTreeFileWriter.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
        String serialized = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        NegoSettings settings = jackson.readValue(serialized, NegoSettings.class);

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
