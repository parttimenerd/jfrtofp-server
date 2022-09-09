package me.bechberger.jfrtofp.server

import io.javalin.Javalin
import io.javalin.http.HttpCode
import io.javalin.http.staticfiles.Location
import me.bechberger.jfrtofp.Config
import me.bechberger.jfrtofp.FileCache
import me.bechberger.jfrtofp.FirefoxProfileGenerator
import me.bechberger.jfrtofp.encodeToZippedStream
import java.io.IOException
import java.net.ServerSocket
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

data class NavigationDestination(
    val packageName: String,
    val className: String,
    val methodNameAndDescriptor: String?,
    val lineNumber: Int?
)

/** starts a server to launch a given file */
class Server(
    val jfrFile: Path,
    val port: Int? = null,
    val navigationHandler: (NavigationDestination) -> Unit = { _ -> },
    /** JFR to convert and serve */
    /** File cache that handles the conversion, or null if no caching is desired */
    val fileCache: FileCache? = null,
    val config: Config = Config()
) {

    val serverPort: Int = port ?: findNewPort()
    val jfrFileFolder = jfrFile.parent
    val jfrFileName = jfrFile.fileName.toString()
    val jfrFileNameWithoutExtension = jfrFileName.substringBeforeLast('.')

    fun run() {
        // see https://github.com/javalin/javalin/issues/358#issuecomment-420982615
        val classLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = Javalin::class.java.classLoader
        val app = Javalin.create { config ->
            val fpResourceFolder = Path.of("src/main/resources/fp")
            config.addStaticFiles { staticFiles ->
                if (Files.exists(fpResourceFolder)) {
                    staticFiles.hostedPath = "/"
                    staticFiles.directory = fpResourceFolder.absolutePathString()
                    staticFiles.location = Location.EXTERNAL
                } else {
                    staticFiles.hostedPath = "/"
                    staticFiles.directory = "/fp"
                    staticFiles.location = Location.CLASSPATH
                }
            }
        }.start(serverPort)
        app.get("/{name}.json.gz") { ctx ->
            val requestedJfrFile = jfrFileFolder.resolve(ctx.queryParam("jfr")!! + ".jfr")
            if (Files.notExists(requestedJfrFile)) {
                ctx.status(HttpCode.NOT_FOUND)
                return@get
            }
            val jsonFileStream = if (fileCache != null) {
                fileCache.get(requestedJfrFile, config).let { Files.newInputStream(it) }
            } else {
                FirefoxProfileGenerator(requestedJfrFile, config).generate().encodeToZippedStream()
            }
            ctx.result(jsonFileStream).header("Content-Encoding", "gzip")
        }
        app.get("/") { ctx ->
            val encodedName = URLEncoder.encode(jfrFileNameWithoutExtension, Charset.defaultCharset())
            ctx.redirect("/index.html/from-url/$encodedName.json.gz")
        }
        app.post("/navigateEditor") {
        }
        Thread.currentThread().contextClassLoader = classLoader
    }

    companion object {
        fun findNewPort(): Int {
            ServerSocket(0).use { socket -> return socket.localPort }
        }

        fun isPortUsable(port: Int): Boolean {
            return try {
                ServerSocket(port).use { _ -> true }
            } catch (_: IOException) {
                false
            }
        }
    }
}
