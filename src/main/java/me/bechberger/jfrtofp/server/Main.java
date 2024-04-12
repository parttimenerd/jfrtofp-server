package me.bechberger.jfrtofp.server;

import me.bechberger.jfrtofp.processor.Config;
import me.bechberger.jfrtofp.processor.ConfigMixin;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;

@Command(name = "jfrtofp-server", mixinStandardHelpOptions = true, description = "Launch a firefox profiler instance "
        + "for a given JFR file")
public class Main implements Runnable {

    @Parameters(index = "0", description = "The JFR file to view")
    private Path file;

    @Option(names = {"-c", "--config"}, description = "Configuration passed directly to the converter", defaultValue
            = "")
    private String config;

    @Option(names = {"-p", "--port"}, description = "Port to run the server on, default is 4243")
    private int port = 0;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    private boolean verbose = false;

    @Override
    public void run() {
        var config = this.config.isBlank() ? ConfigMixin.Companion.parseConfig(new String[]{}) :
                ConfigMixin.Companion.parseConfig(this.config);
        if (this.port == 0) {
            port = Server.findPort();
        }
        var url = verbose ? Server.startIfNeededAndGetUrl(port, file, config, (n) -> n.pkg, (n) -> {
            System.out.println("Navigate to " + n);
        }, verbose) : Server.startIfNeededAndGetUrl(port, file, config, null, null, verbose);
        System.out.println("-------------------------------------------------");
        System.out.println("Navigate to " + url + " to launch the profiler view");
        System.out.println("-------------------------------------------------");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}