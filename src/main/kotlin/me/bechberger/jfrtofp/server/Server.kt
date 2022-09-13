package me.bechberger.jfrtofp.server

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.core.util.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.staticfiles.Location
import me.bechberger.jfrtofp.Config
import me.bechberger.jfrtofp.FileCache
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
    val jfrFileFolder = jfrFile.toAbsolutePath().parent
    val jfrFileName = jfrFile.fileName.toString()
    val jfrFileNameWithoutExtension = jfrFileName.substringBeforeLast('.')

    fun run() {
        val currentFileCache = fileCache ?: FileCache()
        // see https://github.com/javalin/javalin/issues/358#issuecomment-420982615
        val classLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = Javalin::class.java.classLoader
        val restPort = serverPort
        val app = Javalin.create { config ->
            val fpResourceFolder = Path.of("src/main/resources/fp")
            config.addStaticFiles { staticFiles ->
                staticFiles.hostedPath = "/"
                if (Files.exists(fpResourceFolder)) {
                    staticFiles.directory = fpResourceFolder.absolutePathString()
                    staticFiles.location = Location.EXTERNAL
                } else {
                    staticFiles.directory = "/fp"
                    staticFiles.location = Location.CLASSPATH
                }
            }
            config.enableCorsForAllOrigins()
            config.addSinglePageRoot("/", "/fp/index.html")
        }.before { ctx -> ctx.res.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "localhost:$serverPort localhost:$restPort") }
            .start(serverPort)
        app.get("/files/{name}.json.gz") { ctx ->
            val requestedJfrFile = jfrFileFolder.resolve(ctx.pathParam("name") + ".jfr")
            if (Files.notExists(requestedJfrFile)) {
                ctx.redirect("https://http.cat/404")
                return@get
            }
            val jsonFileStream = currentFileCache.get(requestedJfrFile, config).let { Files.newInputStream(it) }
            ctx.result(jsonFileStream)
            ctx.res.setHeader(Header.CONTENT_TYPE, "application/json")
            ctx.res.setHeader(Header.CONTENT_ENCODING, "gzip")
            ctx.res.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        }
        app.get("/default") { ctx ->
            val url = "http://localhost:$restPort/files/$jfrFileNameWithoutExtension.json.gz"
            val encodedName = URLEncoder.encode(url, Charset.defaultCharset())
            println("redirecting to http://localhost:$serverPort/from-url/$encodedName")
            ctx.redirect("http://localhost:$serverPort/from-url/$encodedName")
        }
        app.post("/navigateEditor") {
        }
        Thread.currentThread().contextClassLoader = classLoader
        println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        println("Launch the firefox profiler at http://localhost:$serverPort/default with the selected JFR file")
        println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
        try {
            app.jettyServer()?.server()?.join()
        } catch (_: Exception) {
        } finally {
            if (fileCache == null) {
                currentFileCache.close()
            }
        }
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
