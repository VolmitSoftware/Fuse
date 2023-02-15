package com.volmit.fuse.fabric.management;

import art.arcane.chrono.PrecisionStopwatch;
import art.arcane.multiburst.MultiBurst;
import com.google.gson.Gson;
import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.management.data.Project;
import com.volmit.fuse.fabric.management.data.Workspace;
import com.volmit.fuse.fabric.util.Looper;
import javafx.application.Platform;
import lombok.Getter;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

@Getter
public class FuseService {
    public static final String VERSION = "1.19.3";
    private static final String BUILD_TOOLS_URL = "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";
    private final File fuseDataFolder;
    private final File buildToolsJar;
    private final File buildToolsFolder;
    private final File jdkLocation;
    private final File jdkPackage;
    private final File serverFolder;
    private final JobExecutor executor;
    private final File serverExecutableBuildToolsOut;
    private final File workspaceFile;
    private final File serverPurpurExecutable;
    private final DevServer devServer;
    private final Looper looper;
    private File serverExecutable;
    private boolean ready;
    private boolean jfxready;
    private Workspace workspace;
    private String lastWorkspaceSave;

    public FuseService(File fuseDataFolder) {
        this.fuseDataFolder = fuseDataFolder;
        ready = false;
        jfxready = false;
        workspace = new Workspace();
        executor = new JobExecutor();
        workspaceFile = new File(fuseDataFolder, "workspace.json");
        lastWorkspaceSave = "";
        loadWorkspace();
        buildToolsFolder = new File(fuseDataFolder, "buildtools");
        jdkPackage = new File(fuseDataFolder, "cache/jdkpkg");
        jdkLocation = new File(fuseDataFolder, "jdk");
        buildToolsJar = new File(buildToolsFolder, "BuildTools.jar");
        serverFolder = new File(fuseDataFolder, "dev-server");
        serverExecutable = new File(serverFolder, "spigot-" + VERSION + ".jar");
        serverPurpurExecutable = new File(serverFolder, "purpur-" + VERSION + ".jar");
        serverExecutableBuildToolsOut = new File(buildToolsFolder, VERSION + "/spigot-" + VERSION + ".jar");
        devServer = new DevServer(this::onServerStarted);
        Fuse.log("Fuse Service Initialized with data folder: " + fuseDataFolder.getAbsolutePath());
        Fuse.log("Spigot Version: " + VERSION);
        looper = new Looper() {
            @Override
            protected long loop() {
                onTick();
                return 1000;
            }
        };
    }

    private void installFuseJar() throws IOException {
        String path = Fuse.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        Fuse.log("Running from: " + path);

        if (path.endsWith(".jar")) {
            new File(serverFolder, "plugins/fuse").mkdirs();
            FileUtils.copyFile(new File(path), new File(serverFolder, "plugins/Fuse.jar"));
            Fuse.log("Copied Fuse.jar to server plugins folder");
        } else {
            Fuse.log("Fuse is not running from a jar. Attempting to self build and install from outputs...");
            Project project = new Project(new File(path).getParentFile().getParentFile()
                    .getParentFile().getParentFile().getAbsolutePath());
            Fuse.log("Project: " + project.getLocation());
            project.selfBuild();
        }
    }

    private void onTick() {
        workspace.onTick();

        if (lastWorkspaceSave.equals(new Gson().toJson(workspace))) {
            return;
        }

        saveWorkspace();
    }

    private void loadWorkspace() {
        workspaceFile.getParentFile().mkdirs();
        try {
            workspace = new Gson().fromJson(readAll(workspaceFile), Workspace.class);
            Fuse.log("Loaded Workspace with " + workspace.getProjects().size() + " projects");
        } catch (Throwable e) {
            Fuse.log("Failed to load workspace: " + e.getMessage());
            workspace = new Workspace();
        }
    }

    private void saveWorkspace() {
        String f = new Gson().toJson(workspace);
        lastWorkspaceSave = f;
        saveAll(f, workspaceFile);
        Fuse.log("Saved workspace");
    }

    private String readAll(File file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void saveAll(String s, File file) {
        try {
            Files.write(Paths.get(file.getAbsolutePath()), s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onServerStarted() {
        ready = true;
        Fuse.onServerStarted();
    }

    public void open() {
        MultiBurst.burst.lazy(this::initialize);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void close() {
        saveWorkspace();
        devServer.stopServer();
    }

    private void initialize() {
        executor.queue(() -> {
            try {
                installFuseJar();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        executor.queue(() -> {
            Fuse.log("Initializing Service");
            fuseDataFolder.mkdirs();
            serverFolder.mkdirs();
            installJDK();
            downloadLatestPurpur();
            executor.after(() -> {
                installServerFiles();
                if (!serverPurpurExecutable.exists()) {
                    Fuse.log("Failed to download purpur. Falling back to spigot via build tools.");
                    installBuildTools();
                } else {
                    serverExecutable = serverPurpurExecutable;
                }

                executor.after(() -> {
                    devServer.start();
                    executor.queue(looper::start);
                    executor.queue(Toolkit::getDefaultToolkit);
                    Platform.startup(() -> jfxready = true);
                    executor.queue(() -> {
                        while (!ready) {
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        while (!jfxready) {
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                });
            });
        });
    }

    private void downloadLatestPurpur() {
        executor.queue(() -> {
            Fuse.log("Downloading Purpur " + VERSION);
            download("https://api.purpurmc.org/v2/purpur/" + VERSION + "/latest/download", serverPurpurExecutable);
        });
    }

    private void installBuildTools() {
        executor.queue(() -> {
            Fuse.log("Installing BuildTools");
            buildToolsFolder.mkdirs();
            downloadBuildTools();
        });
    }

    private void downloadBuildTools() {
        executor.queue(() -> {
            if (!buildToolsJar.exists()) {
                Fuse.log("Downloading BuildTools");
                download(BUILD_TOOLS_URL, buildToolsJar);
            }

            buildSpigotJar();
        });
    }

    private void setupSpigotServer() {
        executor.queue(() -> {
            Fuse.log("Setting up Development Spigot Server...");
            installSpigotServerJar();
        });
    }

    private void installServerFiles() {
        executor.queue(() -> installServerFile("bukkit.yml"));
        executor.queue(() -> installServerFile("eula.txt"));
        executor.queue(() -> installServerFile("server.properties"));
        executor.queue(() -> installServerFile("spigot.yml"));
    }

    private void installSpigotServerJar() {
        executor.queue(() -> {
            Fuse.log("Installing Spigot Jar");
            try {
                FileUtils.copyFile(serverExecutableBuildToolsOut, serverExecutable);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void buildSpigotJar() {
        executor.queue(() -> {
            Fuse.log("Building Server Jars (Build Tools)");
            JarExecutor.builder()
                    .directory(buildToolsFolder)
                    .jar(buildToolsJar)
                    .arg("--rev")
                    .arg(VERSION)
                    .arg("--output-dir")
                    .arg(VERSION)
                    .arg("--compile-if-changed")
                    .build().execute();
            setupSpigotServer();
        });
    }

    private void installJDK() {
        executor.queue(() -> {
            Fuse.log("Installing JDK");
            downloadJDK();
        });
    }

    private void downloadJDK() {
        executor.queue(() -> {
            if (!jdkPackage.exists()) {
                Fuse.log("Downloading JDK");
                jdkPackage.getParentFile().mkdirs();
                download(JDKDownloadUrl.getAutoUrl(), jdkPackage);
            }

            extractJDK(!JDKDownloadUrl.getAutoUrl().endsWith("zip"));
        });
    }

    private void extractJDK(boolean tar) {
        executor.queue(() -> {
            Fuse.log("Extracting JDK");
            if (!jdkLocation.exists()) {
                if (tar) {
                    try (InputStream fi = Files.newInputStream(Paths.get(jdkLocation.getAbsolutePath()));
                         InputStream bi = new BufferedInputStream(fi);
                         InputStream gzi = new GzipCompressorInputStream(bi);
                         ArchiveInputStream i = new TarArchiveInputStream(gzi)) {
                        ArchiveEntry entry;
                        while ((entry = i.getNextEntry()) != null) {
                            if (!i.canReadEntryData(entry)) {
                                // log something?
                                continue;
                            }
                            File f = new File(jdkLocation, entry.getName());
                            if (entry.isDirectory()) {
                                if (!f.isDirectory() && !f.mkdirs()) {
                                    throw new IOException("failed to create directory " + f);
                                }
                            } else {
                                File parent = f.getParentFile();
                                if (!parent.isDirectory() && !parent.mkdirs()) {
                                    throw new IOException("failed to create directory " + parent);
                                }
                                try (OutputStream o = Files.newOutputStream(f.toPath())) {
                                    IOUtils.copy(i, o);
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    ZipUtil.unwrap(jdkPackage, jdkLocation);
                }
            }
        });
    }

    public void download(String url, File file) {
        if (file.exists()) {
            Fuse.log("Skipping download of " + file.getName() + " as it already exists.");
            return;
        }

        Fuse.log("Downloading " + url + " into " + file.getPath());
        file.getParentFile().mkdirs();
        PrecisionStopwatch p = PrecisionStopwatch.start();

        try {
            URL at = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(at.openStream());
            FileOutputStream fos = new FileOutputStream(file);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            Fuse.log("Downloaded " + file.getName() + " in " + p.getMilliseconds() + "ms");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void installServerFile(String fileName, String... replacements) {
        try {
            Fuse.log("Installing " + fileName);
            InputStream input = getClass().getResourceAsStream("/server/" + fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter pw = new PrintWriter(new File(serverFolder, fileName));
            String line;

            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < replacements.length; i += 2) {
                    line = line.replaceAll("\\Q$" + replacements[i] + "\\E", replacements[i + 1]);
                }

                pw.println(line);
            }

            reader.close();
            pw.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
