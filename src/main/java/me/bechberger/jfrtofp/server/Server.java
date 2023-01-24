package me.bechberger.jfrtofp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.core.util.Header;
import io.javalin.core.util.JavalinLogger;
import io.javalin.http.staticfiles.Location;
import kotlin.Pair;
import me.bechberger.jfrtofp.FileCache;
import me.bechberger.jfrtofp.processor.Config;
import org.eclipse.jetty.util.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * starts a server to launch a given file
 */
public class Server implements Runnable {

    private final static long DEFAULT_FILE_CACHE_SIZE = 2_000_000_000;
    private final static Logger LOG = Logger.getLogger("Server");

    public static class JFRFileInfo {
        private final Path file;
        @Nullable
        private Config config;

        public JFRFileInfo(Path file,
                           @Nullable Config config) {
            this.file = file;
            this.config = config;
        }
    }

    private final int port;
    private final Map<String, JFRFileInfo> registeredFiles = new HashMap<>();
    private final Map<Path, Pair<String, JFRFileInfo>> fileToId = new HashMap<>();
    private final FileCache fileCache;
    private volatile Config config;

    @Nullable
    private final Function<ClassLocation, String> fileGetter;
    @Nullable
    private final Consumer<NavigationDestination> navigate;

    private final boolean verbose;

    public Server(int port, long fileCacheSize, Config config, boolean verbose) {
        this(port, fileCacheSize, config, null, null, verbose);
    }

    /**
     * port == -1: choose new, fileCacheSize == -1: default
     */
    public Server(int port, long fileCacheSize, Config config,
                  @Nullable Function<ClassLocation, String> fileGetter,
                  @Nullable Consumer<NavigationDestination> navigate, boolean verbose) {
        long size = fileCacheSize != -1 ? fileCacheSize : DEFAULT_FILE_CACHE_SIZE;
        this.fileCache = new FileCache(null, size, ".json.gz");
        this.config = config;
        this.port = port == -1 ? findNewPort() : port;
        this.fileGetter = fileGetter;
        this.navigate = navigate;
        this.verbose = verbose;
        LOG.setLevel(verbose ? Level.INFO : Level.WARNING);
        if (!verbose) {
            JavalinLogger.enabled = false;
        }
    }

    public int getPort() {
        return port;
    }

    void modfiyConfig(Config config) {
        if (navigate != null) {
            config.setSourceUrl("post|http://localhost:" + port + "/navigate");
        } else if (fileGetter != null) {
            config.setSourceUrl("http://localhost:" + port + "/navigate");
        } else {
            config.setSourceUrl(null);
        }
    }

    private static Pair<String, String> splitPathIntoPkgAndClass(String matchedPath, String path) {
        var fullyQualified = path.substring(matchedPath.length());
        var parts = Arrays.asList(fullyQualified.split("[.]"));
        var pkg = parts.stream().limit(parts.size() - 2).collect(Collectors.joining("."));
        var klass = parts.get(parts.size() - 2);
        return new Pair<>(pkg, klass);
    }

    @Override
    public void run() {
        Log.getProperties().setProperty("org.eclipse.jetty.util.log.announce", "false");
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
                        new JFRFileInfo(Path.of(name + ".jfr"), new Config()));
                if (Files.notExists(requestedFile.file)) {
                    ctx.redirect("https://http.cat/404");
                    return;
                }
                var config = requestedFile.config != null ? requestedFile.config : this.config;
                modfiyConfig(config);
                LOG.info("Processing " + requestedFile.file.toFile());
                ctx.result(Files.newInputStream(fileCache.get(requestedFile.file, config)));
                ctx.res.setHeader(Header.CONTENT_TYPE, "application/json");
                ctx.res.setHeader(Header.CONTENT_ENCODING, "gzip");
                ctx.res.setHeader(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            });
            modfiyConfig(config);
            if (navigate != null) {
                app.post("/navigate/*", ctx -> {
                    var pkgAndClass = splitPathIntoPkgAndClass("/navigate/", ctx.path());
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(ctx.body());
                    var destination = new NavigationDestination(pkgAndClass.getFirst(), pkgAndClass.getSecond(),
                            jsonNode.get("method").asText().split("[.]", 2)[1], jsonNode.get("line").asInt(-1));
                    LOG.info("Navigating to " + destination);
                    navigate.accept(destination);
                    ctx.result("ok");
                });
            }
            if (fileGetter != null) {
                app.get("/navigate/*", ctx -> {
                    var pkgAndClass = splitPathIntoPkgAndClass("/navigate/", ctx.path());
                    var destination = new ClassLocation(pkgAndClass.getFirst(), pkgAndClass.getSecond());
                    LOG.info("Getting file " + destination);
                    var result = fileGetter.apply(destination);
                    ctx.result(result);
                });
            }
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
        registeredFiles.put(newName, new JFRFileInfo(file, config));
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

    public static synchronized Server getInstance(long fileCacheSize, @Nullable Config config, boolean verbose) {
        if (instance == null) {
            instance = new Server(-1, fileCacheSize, config, verbose);
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
        return getInstance(-1, null, false);
    }
}
