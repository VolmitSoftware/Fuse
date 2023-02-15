package com.volmit.fuse.fabric.management;

import com.volmit.fuse.fabric.Fuse;
import com.volmit.fuse.fabric.util.ProcessRelogger;
import lombok.Builder;
import lombok.Singular;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Builder
public class GradleExecutor extends Thread {
    private final File project;
    private final File gradle;
    @Singular
    private final List<String> args;
    @Builder.Default
    public int code = -1;

    public GradleExecutor execute() {
        start();
        try {
            join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    public void run() {
        String gradleRoot = new File(gradle, "bin/gradle").getAbsolutePath();
        List<String> args = new ArrayList<>();
        args.add("cmd");
        args.add("/c");
        args.add(gradleRoot);
        args.add("-Dorg.gradle.java.home=" + Fuse.service.getJdkLocation().getAbsolutePath());

        if (this.args != null) {
            args.addAll(this.args);
        }

        Fuse.log("Executing: " + String.join(" ", args) + " in " + project.getAbsolutePath());
        Fuse.log(Fuse.service.getFuseDataFolder().getAbsolutePath());
        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .command(args.toArray(new String[0]))
                    .directory(project);
            pb.environment().put("JAVA_HOME", Fuse.service.getJdkLocation().getAbsolutePath());
            Process p = pb.start();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
            new ProcessRelogger(p.getInputStream(), false).start();
            new ProcessRelogger(p.getErrorStream(), false).start();
            code = p.waitFor();
            Fuse.log("Process Exited With Code " + code);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
