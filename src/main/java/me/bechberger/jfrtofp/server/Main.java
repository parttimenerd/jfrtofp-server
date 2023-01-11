package me.bechberger.jfrtofp.server;

import me.bechberger.jfrtofp.processor.ConfigMixin;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import java.nio.file.Path;

import static me.bechberger.jfrtofp.server.Server.findNewPort;
import static me.bechberger.jfrtofp.server.Server.isPortUsable;

@Command(
    name = "jfrtofp-server",
    mixinStandardHelpOptions = true,
    description = "Launch a firefox profiler instance for a given JFR file"
)
public class Main implements Runnable {

    public static final int DEFAULT_PORT = 4243;

    @Parameters(index = "0", description = "The JFR file to view")
    private Path file;

    @Option(names = {"-c", "--config"}, description = "Configuration passed directly to the converter", defaultValue = "")
    private String config;

    @Option(names = {"-p", "--port"}, description = "Port to run the server on, default is 4243")
    private int port = 0;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose = false;

    @Override
    public void run() {
        var config = ConfigMixin.Companion.parseConfig(this.config);
        if (this.port == 0) {
            if (isPortUsable(DEFAULT_PORT)) {
                port = DEFAULT_PORT;
            } else {
                port = findNewPort();
            }
        }
        var server = new Server(port, -1, config, (n) -> n.file, null, verbose);
        var name = server.registerJFRFile(file, null, config);
        System.out.println("-------------------------------------------------");
        System.out.println("Navigate to " + server.getFirefoxProfilerURL(name) + " to launch the profiler view");
        System.out.println("-------------------------------------------------");
        server.run();
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}