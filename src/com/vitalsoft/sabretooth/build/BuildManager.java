package com.vitalsoft.sabretooth.build;

import com.vitalsoft.sabretooth.config.SabreConf;
import com.vitalsoft.sabretooth.project.Project;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class BuildManager {
    private Process currentProcess;
    private boolean running = false;

    public enum BuildTask { ASSEMBLE_DEBUG, ASSEMBLE_RELEASE, CLEAN, BUILD, CUSTOM }

    public static class BuildResult {
        public final boolean success;
        public final List<String> output;
        public final String apkPath;

        BuildResult(boolean success, List<String> output, String apkPath) {
            this.success = success;
            this.output = output;
            this.apkPath = apkPath;
        }
    }

    /**
     * Start a build asynchronously.
     * @param project The project to build
     * @param task The build task
     * @param outputCallback Called with each line of output (on a background thread)
     * @param onFinish Called when build completes (may be on background thread, dispatch to EDT)
     */
    public void buildAsync(Project project, BuildTask task, Consumer<String> outputCallback,
                           Consumer<BuildResult> onFinish) {
        if (running) {
            outputCallback.accept("[SabreTooth] Build already in progress.");
            return;
        }

        Thread t = new Thread(() -> {
            running = true;
            BuildResult result = null;
            try {
                result = runBuild(project, task, outputCallback);
            } catch (Exception e) {
                outputCallback.accept("[SabreTooth] Build error: " + e.getMessage());
                result = new BuildResult(false, Collections.emptyList(), null);
            } finally {
                running = false;
                if (onFinish != null) onFinish.accept(result);
            }
        }, "SabreTooth-Build");
        t.setDaemon(true);
        t.start();
    }

    private BuildResult runBuild(Project project, BuildTask task, Consumer<String> out) throws Exception {
        SabreConf conf = project.getConf();
        String gradlePath = conf.get(SabreConf.KEY_GRADLE_PATH);
        String jdkPath    = conf.get(SabreConf.KEY_JDK_PATH);
        String projectType = conf.get(SabreConf.KEY_PROJECT_TYPE);
        String buildType  = conf.get(SabreConf.KEY_BUILD_TYPE);
        String extraArgs  = conf.get(SabreConf.KEY_EXTRA_GRADLE_ARGS);

        File workDir = project.getRootDir();

        List<String> cmd = new ArrayList<>();

        if ("eclipse".equalsIgnoreCase(projectType)) {
            return buildEclipseStyle(project, task, out);
        }

        // Gradle build
        if (gradlePath == null || gradlePath.isEmpty()) {
            // Try system gradle or gradlew
            File gradlew = new File(workDir, "gradlew");
            if (!gradlew.exists()) gradlew = new File(workDir, "gradlew.bat");
            if (gradlew.exists()) {
                gradlePath = gradlew.getAbsolutePath();
                gradlew.setExecutable(true);
            } else {
                gradlePath = findGradleOnPath();
                if (gradlePath == null) {
                    out.accept("[SabreTooth] ERROR: No Gradle found. Please set Gradle path in Project Settings.");
                    return new BuildResult(false, Collections.emptyList(), null);
                }
            }
        }

        cmd.add(gradlePath);

        switch (task) {
            case ASSEMBLE_DEBUG   -> cmd.add(":app:assembleDebug");
            case ASSEMBLE_RELEASE -> cmd.add(":app:assembleRelease");
            case CLEAN            -> cmd.add("clean");
            case BUILD            -> cmd.add("build");
            case CUSTOM           -> {
                if (extraArgs != null && !extraArgs.isEmpty()) {
                    cmd.addAll(Arrays.asList(extraArgs.split("\\s+")));
                } else {
                    cmd.add(":app:assembleDebug");
                }
            }
        }

        cmd.add("--console=plain");
        if (extraArgs != null && !extraArgs.isEmpty() && task != BuildTask.CUSTOM) {
            cmd.addAll(Arrays.asList(extraArgs.split("\\s+")));
        }

        out.accept("[SabreTooth] Working dir: " + workDir.getAbsolutePath());
        out.accept("[SabreTooth] Command: " + String.join(" ", cmd));
        out.accept("─────────────────────────────────────────");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir);
        pb.redirectErrorStream(true);

        // Set JDK if specified
        if (jdkPath != null && !jdkPath.isEmpty()) {
            Map<String, String> env = pb.environment();
            env.put("JAVA_HOME", jdkPath);
            String pathSep = File.pathSeparator;
            String jdkBin = jdkPath + File.separator + "bin";
            String currentPath = env.getOrDefault("PATH", "");
            env.put("PATH", jdkBin + pathSep + currentPath);
        }

        currentProcess = pb.start();
        List<String> allOutput = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.accept(line);
                allOutput.add(line);
            }
        }

        int exitCode = currentProcess.waitFor();
        out.accept("─────────────────────────────────────────");

        boolean success = exitCode == 0;
        String apkPath = null;

        if (success) {
            apkPath = findGeneratedApk(project, task);
            out.accept("[SabreTooth] ✓ BUILD SUCCESSFUL");
            if (apkPath != null) {
                out.accept("[SabreTooth] APK: " + apkPath);
            }
        } else {
            out.accept("[SabreTooth] ✗ BUILD FAILED (exit code " + exitCode + ")");
        }

        return new BuildResult(success, allOutput, apkPath);
    }

    private BuildResult buildEclipseStyle(Project project, BuildTask task, Consumer<String> out) throws Exception {
        // Eclipse-style build using ant if available, or pure Java compilation
        out.accept("[SabreTooth] Eclipse-style build not fully implemented.");
        out.accept("[SabreTooth] Please convert to Gradle or use Gradle wrapper.");
        return new BuildResult(false, Collections.emptyList(), null);
    }

    private String findGradleOnPath() {
        try {
            String path = System.getenv("PATH");
            if (path == null) return null;
            for (String dir : path.split(File.pathSeparator)) {
                File f = new File(dir, "gradle");
                if (f.exists() && f.canExecute()) return f.getAbsolutePath();
                f = new File(dir, "gradle.bat");
                if (f.exists()) return f.getAbsolutePath();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String findGeneratedApk(Project project, BuildTask task) {
        File root = project.getRootDir();
        String[] paths;
        if (task == BuildTask.ASSEMBLE_RELEASE) {
            paths = new String[]{
                "app/build/outputs/apk/release/app-release.apk",
                "app/build/outputs/apk/release/app-release-unsigned.apk",
                "build/outputs/apk/release/app-release.apk",
            };
        } else {
            paths = new String[]{
                "app/build/outputs/apk/debug/app-debug.apk",
                "build/outputs/apk/debug/app-debug.apk",
                "app/build/outputs/apk/app-debug.apk",
            };
        }
        for (String p : paths) {
            File f = new File(root, p);
            if (f.exists()) return f.getAbsolutePath();
        }
        // Search recursively in outputs dir
        File outputs = new File(root, "app/build/outputs/apk");
        if (!outputs.exists()) outputs = new File(root, "build/outputs/apk");
        if (outputs.exists()) {
            return findApkRecursive(outputs);
        }
        return null;
    }

    private String findApkRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".apk")) return f.getAbsolutePath();
            if (f.isDirectory()) {
                String r = findApkRecursive(f);
                if (r != null) return r;
            }
        }
        return null;
    }

    public void cancel() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
        }
        running = false;
    }

    public boolean isRunning() { return running; }
}
