package me.bechberger.jfrtofp.server;

import io.javalin.Javalin;
import io.javalin.core.util.Header;
import io.javalin.http.staticfiles.Location;
import kotlin.Pair;
import me.bechberger.jfrtofp.Config;
import me.bechberger.jfrtofp.FileCache;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import me.bechberger.jfrtofp.FirefoxProfileGenerator;
import me.bechberger.jfrtofp.ProcessorKt;
import org.jetbrains.annotations.Nullable;


/**
 * starts a server to launch a given file
 */
public class Server implements Runnable {

    private final static long DEFAULT_FILE_CACHE_SIZE = 2_000_000_000;
    private final static Logger LOG = Logger.getLogger("Server");

    public static class JFRFileInfo {
        private final Path file;
        @Nullable
        private Consumer<NavigationDestination> navigationHelper;
        @Nullable
        private Config config;

        public JFRFileInfo(Path file, @Nullable Consumer<NavigationDestination> navigationHelper,
                           @Nullable Config config) {
            this.file = file;
            this.navigationHelper = navigationHelper;
            this.config = config;
        }
    }

    private final int port;
    private final Map<String, JFRFileInfo> registeredFiles = new HashMap<>();
    private final Map<Path, Pair<String, JFRFileInfo>> fileToId = new HashMap<>();
    private final FileCache fileCache;
    private volatile Config config;

    /**
     * port == -1: choose new, fileCacheSize == -1: default
     */
    public Server(int port, long fileCacheSize, Config config) {
        long size = fileCacheSize != -1 ? fileCacheSize : DEFAULT_FILE_CACHE_SIZE;
        this.fileCache = new FileCache(null, size, ".json.gz");
        this.config = config;
        this.port = port == -1 ? findNewPort() : port;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (fileCache != null) {
                fileCache.close();
            }
        }));
        // see https://github.com/javalin/javalin/issues/358#issuecomment-420982615
        var classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Javalin.class.getClassLoader());
        try (var app = Javalin.create(config -> {
            var fpResourceFolder = Path.of("src/main/resources/fp");
            if (Files.exists(fpResourceFolder)) {
                config.addStaticFiles(fpResourceFolder.toAbsolutePath().toString(), Location.EXTERNAL);
            } else {
                config.addStaticFiles("/fp", Location.CLASSPATH);
            }
            config.enableCorsForAllOrigins();
            config.addSinglePageRoot("/", "/fp/index.html");
        })) {
            app.before(ctx -> ctx.res.setHeader(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "localhost:$serverPort " +
                    "localhost:$restPort"));
            app.get("/files/{name}.json.gz", ctx -> {
                var name = ctx.pathParam("name");
                var requestedFile = registeredFiles.getOrDefault(name,
                        new JFRFileInfo(Path.of(name + ".jfr"), null,
                        null));
                if (Files.notExists(requestedFile.file)) {
                    ctx.redirect("https://http.cat/404");
                    return;
                }
                var config = requestedFile.config != null ? requestedFile.config : this.config;
                LOG.info("Processing " + requestedFile.file.toFile());
                ctx.result(Files.newInputStream(fileCache.get(requestedFile.file, config)));
                ctx.res.setHeader(Header.CONTENT_TYPE, "application/json");
                ctx.res.setHeader(Header.CONTENT_ENCODING, "gzip");
                ctx.res.setHeader(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            });
            app.start(port);
            app.get("/show/{name}", ctx -> {
                var targetUrl = getFirefoxProfilerURL(ctx.pathParam("name"));
                System.out.printf("Redirecting to " + targetUrl + "\n");
                ctx.redirect(targetUrl);
            });
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                Objects.requireNonNull(app.jettyServer()).server().join();
            } catch (Exception ignored) {
            }
        }
    }

    public String getJSONURL(String name) {
        return String.format("http://localhost:%d/files/%s.json.gz", port, name);
    }

    String getFirefoxProfilerURL(String name) {
        return String.format("http://localhost:%d/from-url/%s", port,
                URLEncoder.encode(getJSONURL(name), Charset.defaultCharset()));
    }

    public static int findNewPort() {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPortUsable(int port) {
        try (var socket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    String registerJFRFile(Path file, @Nullable Consumer<NavigationDestination> navigationHelper,
                                  @Nullable Config config) {
        if (fileToId.containsKey(file)) {
            var p = fileToId.get(file);
            p.getSecond().navigationHelper = navigationHelper;
            p.getSecond().config = config;
            return p.getFirst();
        }
        var name = file.getFileName().toString();
        if (name.endsWith(".jfr")) {
            name = name.substring(0, name.length() - 4);
        }
        var i = 0;
        var newName = name;
        while (registeredFiles.containsKey(newName)) {
            newName = name + "_" + i;
            i++;
        }
        registeredFiles.put(newName, new JFRFileInfo(file, navigationHelper, config));
        fileToId.put(file, new Pair<>(newName, registeredFiles.get(newName)));
        return newName;
    }

    public void setCacheSize(long newSize) {
        fileCache.setMaxSize(newSize);
    }

    public long getCacheSize() {
        return fileCache.getMaxSize();
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    private static Server instance;

    public static synchronized Server getInstance(long fileCacheSize, @Nullable Config config) {
        if (instance == null) {
            instance = new Server(-1, fileCacheSize, config);
            new Thread(instance).start();
        } else {
            if (fileCacheSize != -1) {
                instance.setCacheSize(fileCacheSize);
            }
            instance.config = config;
        }
        return instance;
    }

    public static Server getInstance() {
        return getInstance(-1, null);
    }

    /** Returns the url for the passed JFR file and starts the server if needed */
    public static String getURLForFile(Path file, @Nullable Consumer<NavigationDestination> navigationHelper,
                                       @Nullable Config config) {
        var server = getInstance();
        return server.getFirefoxProfilerURL(server.registerJFRFile(file, navigationHelper, config));
    }
}
