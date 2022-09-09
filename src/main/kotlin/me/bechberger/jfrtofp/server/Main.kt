package me.bechberger.jfrtofp.server

import me.bechberger.jfrtofp.server.Server.Companion.findNewPort
import me.bechberger.jfrtofp.server.Server.Companion.isPortUsable
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path
import kotlin.system.exitProcess

@Command(
    name = "jfrtofp-server",
    mixinStandardHelpOptions = true,
    description = ["Launch a firefox profiler instance for a given JFR file"]
)
class Main : Runnable {

    companion object {
        const val DEFAULT_PORT = 4243
    }

    @Parameters(index = "0", description = ["The JFR file to view"])
    lateinit var file: Path

    @Option(names = ["-c", "--config"], description = ["Configuration passed directly to the converter"])
    var config: String? = null

    @Option(names = ["-p", "--port"], description = ["Port to run the server on, default is 4243"])
    var port: Int? = null

    override fun run() {
        val config = me.bechberger.jfrtofp.ConfigMixin.parseConfig(config ?: "")
        if (port == null) {
            if (isPortUsable(DEFAULT_PORT)) {
                port = DEFAULT_PORT
            } else {
                port = findNewPort()
            }
        }
        Server(file, port, config = config).run()
    }
}

@Suppress("SpreadOperator")
fun main(args: Array<String>): Unit = exitProcess(CommandLine(Main()).execute(*args))
