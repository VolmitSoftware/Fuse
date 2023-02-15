package com.volmit.fuse.fabric.management.data;

import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.management.GradleExecutor;
import com.volmit.fuse.fabric.util.FolderWatcher;
import lombok.Data;
import org.simpleyaml.configuration.file.YamlFile;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Project {
    private String name;
    private String location;
    private List<String> watchList;
    private List<String> softDepend;
    private List<String> depend;
    private String main;
    private transient boolean building;
    private transient Map<String, FolderWatcher> _watchers;

    public Project(String location) {
        this.location = location;
        this.watchList = new ArrayList<>();
        watchList.add(new File(location, "src").getAbsolutePath());
        watchList.add(new File(location, "gradle.properties").getAbsolutePath());
        watchList.add(new File(location, "settings.gradle").getAbsolutePath());
        watchList.add(new File(location, "build.gradle").getAbsolutePath());
    }

    public void onTick() {
        boolean modified = updateWatchers();
        modified |= name == null || name.isEmpty();
        modified |= main == null || main.isEmpty();
        modified |= checkWatchers();

        if(modified) {
            building = true;
            Fuse.onProjectBuildStarted(this);
            Fuse.service.getExecutor().queueThenWait(() -> {
                try {
                    build();
                    Fuse.onProjectBuildSuccess(this);
                } catch( Throwable e) {
                    Fuse.onProjectBuildFailed(this);
                    e.printStackTrace();
                }
            });
            building = false;
        }
    }

    private String getGradleDownloadUrl() {
        File gradleWrapperConfig = new File(location, "gradle/wrapper/gradle-wrapper.properties");
        if(gradleWrapperConfig.exists()) {
            try {
                String[] lines = Files.readAllLines(gradleWrapperConfig.toPath()).toArray(new String[0]);
                for(String i : lines) {
                    if(i.startsWith("distributionUrl=")) {
                        return i.split("=")[1].replaceAll("\\Q\\\\E", "");
                    }
                }
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }

        Fuse.err("Missing gradle file. Checking parent directory for gradle-wrapper.properties");

        gradleWrapperConfig = new File(new File(location).getParentFile(), "gradle/wrapper/gradle-wrapper.properties");
        if(gradleWrapperConfig.exists()) {
            try {
                String[] lines = Files.readAllLines(gradleWrapperConfig.toPath()).toArray(new String[0]);
                for(String i : lines) {
                    if(i.startsWith("distributionUrl=")) {
                        return i.split("=")[1].replaceAll("\\Q\\\\E", "");
                    }
                }
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getName()
    {
        return name == null ? new File(location).getName() : name;
    }

    private String getGradleVersion(String url) {
        String[] seg = url.split("\\Q/\\E");
        String n = seg[seg.length - 1].replaceAll("\\Q-bin.zip\\E", "")
            .replaceAll("\\Qgradle-\\E", "");
        return n;
    }

    private File getGradleInstallationFolder(String url) {
        return new File(Fuse.service.getFuseDataFolder(), "gradle/" + getGradleVersion(url));
    }

    private void installGradle() {
        String url = getGradleDownloadUrl();
        File folder = getGradleInstallationFolder(url);
        File dll = new File(folder.getParentFile(), "downloads/gradle-" + getGradleVersion(url) + ".zip");
        dll.getParentFile().mkdirs();

        if(!folder.exists()) {
            folder.mkdirs();
            Fuse.service.download(url, dll);
            Fuse.log("Installing Gradle " + getGradleVersion(url) + " to " + folder.getAbsolutePath());
            ZipUtil.unwrap(dll, folder);
        }
    }

    public void selfBuild() throws IOException {
        Fuse.log("Building " + new File(location).getName());
        installGradle();
        GradleExecutor.builder()
            .project(new File(location))
            .gradle(getGradleInstallationFolder(getGradleDownloadUrl()))
            .arg("remapJar")
            .build().execute();
        File file = findBestOutput(false);
        Fuse.log("SelfBuild complete");

        if(file != null) {
            Fuse.service.getDevServer().installFuse(file);
        } else {
            Fuse.log("No output found");
        }
    }

    private void build() throws IOException {
        Fuse.log("Building " + new File(location).getName());
        boolean hasShadow = Files.readAllLines(new File(location, "build.gradle").toPath()).stream()
            .anyMatch(i -> i.contains("com.github.johnrengelman.shadow"));
        installGradle();

        if(hasShadow) {
            Fuse.log("Shadow detected, building shadow jar");
        }

        GradleExecutor e = GradleExecutor.builder()
            .project(new File(location))
            .gradle(getGradleInstallationFolder(getGradleDownloadUrl()))
            .arg(hasShadow ? "shadowJar" : "build")
            .build().execute();

        if(e.code != 0)
        {
            Fuse.log("Gradle build failed with code " + e.code);
            throw new RuntimeException("Gradle build failed with code " + e.code);
        }

        File file = findBestOutput(hasShadow);
        Fuse.log("Build complete");

        if(file != null) {
            Fuse.log("Updating Properties for " + new File(location).getName());
            updatePropertiesFromJar(file);
            Fuse.log("Installing " + name);
            Fuse.service.getDevServer().installPlugin(this, file);
        } else {
            Fuse.log("No output found");
        }
    }

    private void updatePropertiesFromJar(File file) throws IOException {
        String pluginYml = new String(ZipUtil.unpackEntry(file, "plugin.yml"));
        YamlFile yml = YamlFile.loadConfigurationFromString(pluginYml);
        name = yml.getString("name");
        main = yml.getString("main");

        if(yml.contains("depend")) {
            depend = yml.getStringList("depend");
        }
        if(yml.contains("softdepend")) {
            softDepend = yml.getStringList("softdepend");
        }

        Fuse.log("Updated properties from built jar");
    }

    private File findBestOutput(boolean shadow) {
        File buildLibs = new File(location, "build/libs");
        List<File> found = new ArrayList<>();
        for(File i : buildLibs.listFiles()) {
            if(i.getName().endsWith(shadow ? "-all.jar" : ".jar")) {
                found.add(i);
            }
        }

        if(found.isEmpty()) {
            return null;
        }

        found.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return found.get(0);
    }

    private boolean checkWatchers() {
        boolean modified = false;
        for(FolderWatcher i : _watchers.values()) {
            modified |= i.checkModifiedFast();
        }

        return modified;
    }

    private boolean updateWatchers() {
        List<String> removals = new ArrayList<>();
        List<String> adds = new ArrayList<>();

        if(_watchers == null) {
            _watchers = new HashMap<>();
        }

        for(String i : _watchers.keySet()) {
            if(!watchList.contains(i)) {
                removals.add(i);
            }
        }

        for(String i : watchList) {
            if(!_watchers.containsKey(i)) {
                adds.add(i);
            }
        }

        for(String i : removals) {
            _watchers.remove(i);
        }

        for(String i : adds) {
            _watchers.put(i, new FolderWatcher(new File(i)));
        }

        return !removals.isEmpty() || !adds.isEmpty();
    }
}
