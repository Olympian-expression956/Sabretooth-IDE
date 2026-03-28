package com.vitalsoft.sabretooth.util;

import java.io.*;
import java.util.*;
import java.nio.file.*;

public class ToolDetector {

    public static class ToolInfo {
        public final String path;
        public final String version;
        public final String label;

        public ToolInfo(String path, String version) {
            this.path = path;
            this.version = version;
            this.label = new File(path).getName() + " (" + version + ") - " + path;
        }

        @Override
        public String toString() { return label; }
    }

    /**
     * Find all Gradle installations on the system.
     */
    public static List<ToolInfo> findGradleInstallations() {
        List<ToolInfo> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. From PATH
        addFromPath("gradle", found, seen);

        // 2. GRADLE_HOME env
        String gradleHome = System.getenv("GRADLE_HOME");
        if (gradleHome != null && !gradleHome.isEmpty()) {
            String bin = gradleHome + File.separator + "bin" + File.separator + "gradle";
            addGradle(bin, found, seen);
        }

        // 3. Common install dirs
        String home = System.getProperty("user.home");
        List<String> candidates = new ArrayList<>();
        candidates.add(home + "/.sdkman/candidates/gradle");
        candidates.add(home + "/.gradle/wrapper/dists");
        candidates.add("/opt/gradle");
        candidates.add("/usr/local/gradle");
        candidates.add("C:\\Gradle");
        candidates.add(home + "\\scoop\\apps\\gradle");

        // SDKMAN style: ~/.sdkman/candidates/gradle/X.Y.Z/bin/gradle
        for (String base : candidates) {
            File baseDir = new File(base);
            if (!baseDir.exists()) continue;
            if (base.contains("wrapper/dists")) {
                // Gradle wrapper dists - nested structure
                scanGradleWrapperDists(baseDir, found, seen);
            } else if (base.contains("sdkman")) {
                File[] versions = baseDir.listFiles(File::isDirectory);
                if (versions != null) {
                    for (File v : versions) {
                        if ("current".equals(v.getName())) continue;
                        addGradle(v.getAbsolutePath() + "/bin/gradle", found, seen);
                    }
                }
            } else {
                File[] versions = baseDir.listFiles(File::isDirectory);
                if (versions != null) {
                    for (File v : versions) {
                        addGradle(v.getAbsolutePath() + "/bin/gradle", found, seen);
                    }
                }
            }
        }

        return found;
    }

    private static void scanGradleWrapperDists(File distsDir, List<ToolInfo> found, Set<String> seen) {
        File[] versions = distsDir.listFiles(File::isDirectory);
        if (versions == null) return;
        for (File vDir : versions) {
            // Inside each version dir is a hash dir, then gradle-X.Y.Z-bin
            File[] hashes = vDir.listFiles(File::isDirectory);
            if (hashes == null) continue;
            for (File hash : hashes) {
                File[] installs = hash.listFiles(File::isDirectory);
                if (installs == null) continue;
                for (File install : installs) {
                    addGradle(install.getAbsolutePath() + "/bin/gradle", found, seen);
                }
            }
        }
    }

    private static void addFromPath(String cmd, List<ToolInfo> found, Set<String> seen) {
        try {
            String[] pathDirs = System.getenv("PATH").split(File.pathSeparator);
            for (String dir : pathDirs) {
                File f = new File(dir, cmd);
                if (!f.exists()) f = new File(dir, cmd + ".bat");
                if (f.exists() && f.canExecute()) {
                    addGradle(f.getAbsolutePath(), found, seen);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void addGradle(String path, List<ToolInfo> found, Set<String> seen) {
        File f = new File(path);
        if (!f.exists()) {
            // also try .bat and .cmd
            f = new File(path + ".bat");
            if (!f.exists()) f = new File(path + ".cmd");
            if (!f.exists()) return;
        }
        try {
            String canonical = f.getCanonicalPath();
            if (seen.contains(canonical)) return;
            seen.add(canonical);
            String version = getGradleVersion(f.getAbsolutePath());
            found.add(new ToolInfo(f.getAbsolutePath(), version));
        } catch (IOException ignored) {}
    }

    private static String getGradleVersion(String gradlePath) {
        try {
            Process p = new ProcessBuilder(gradlePath, "--version")
                .redirectErrorStream(true)
                .start();
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Gradle ")) return line.substring(7).trim();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    /**
     * Find all JDK installations.
     */
    public static List<ToolInfo> findJdkInstallations() {
        List<ToolInfo> found = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Current JVM
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) addJdk(javaHome, found, seen);

        // JAVA_HOME env
        String envJavaHome = System.getenv("JAVA_HOME");
        if (envJavaHome != null && !envJavaHome.isEmpty()) addJdk(envJavaHome, found, seen);

        // Common JDK base dirs
        String home = System.getProperty("user.home");
        List<String> baseDirs = new ArrayList<>(Arrays.asList(
            "/usr/lib/jvm",
            "/usr/java",
            "/opt/jdk",
            "/opt/java",
            home + "/.sdkman/candidates/java",
            "C:\\Program Files\\Java",
            "C:\\Program Files\\Eclipse Adoptium",
            "C:\\Program Files\\Microsoft",
            "C:\\Program Files\\BellSoft",
            home + "\\scoop\\apps\\temurin17-jdk",
            "/Library/Java/JavaVirtualMachines"
        ));

        for (String base : baseDirs) {
            File baseDir = new File(base);
            if (!baseDir.exists()) continue;
            File[] entries = baseDir.listFiles(File::isDirectory);
            if (entries == null) continue;
            for (File entry : entries) {
                if ("current".equals(entry.getName())) continue;
                // macOS style: foo.jdk/Contents/Home
                File macHome = new File(entry, "Contents/Home");
                if (macHome.exists()) addJdk(macHome.getAbsolutePath(), found, seen);
                else addJdk(entry.getAbsolutePath(), found, seen);
            }
        }

        return found;
    }

    private static void addJdk(String path, List<ToolInfo> found, Set<String> seen) {
        // Normalize: if path ends with /jre, go up
        File f = new File(path);
        if (!f.exists()) return;
        try {
            String canonical = f.getCanonicalPath();
            if (seen.contains(canonical)) return;
            seen.add(canonical);
            // Must have bin/java
            File java = new File(f, "bin/java");
            if (!java.exists()) java = new File(f, "bin/java.exe");
            if (!java.exists()) return;
            String version = getJavaVersion(java.getAbsolutePath());
            found.add(new ToolInfo(f.getAbsolutePath(), version));
        } catch (IOException ignored) {}
    }

    private static String getJavaVersion(String javaPath) {
        try {
            Process p = new ProcessBuilder(javaPath, "-version")
                .redirectErrorStream(true)
                .start();
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            if (line != null) {
                // java version "17.0.2" or openjdk version "21.0.1"
                int q1 = line.indexOf('"');
                int q2 = line.lastIndexOf('"');
                if (q1 >= 0 && q2 > q1) return line.substring(q1 + 1, q2);
                return line;
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    /**
     * Find Android SDK android.jar files given an SDK root.
     */
    public static List<String> findAndroidJars(String sdkRoot) {
        List<String> jars = new ArrayList<>();
        if (sdkRoot == null || sdkRoot.isEmpty()) return jars;
        File platforms = new File(sdkRoot, "platforms");
        if (!platforms.exists()) return jars;
        File[] versions = platforms.listFiles(File::isDirectory);
        if (versions == null) return jars;
        Arrays.sort(versions, (a, b) -> {
            int na = extractApiLevel(a.getName());
            int nb = extractApiLevel(b.getName());
            return Integer.compare(nb, na); // descending
        });
        for (File v : versions) {
            File jar = new File(v, "android.jar");
            if (jar.exists()) jars.add(jar.getAbsolutePath());
        }
        return jars;
    }

    private static int extractApiLevel(String name) {
        // android-34 -> 34
        try {
            return Integer.parseInt(name.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) { return 0; }
    }

    public static String detectAndroidSdkRoot() {
        String[] envVars = {"ANDROID_HOME", "ANDROID_SDK_ROOT"};
        for (String v : envVars) {
            String val = System.getenv(v);
            if (val != null && !val.isEmpty() && new File(val).exists()) return val;
        }
        String home = System.getProperty("user.home");
        String[] candidates = {
            home + "/Android/Sdk",
            home + "/android/sdk",
            home + "/.android/sdk",
            "/opt/android-sdk",
            "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Android\\Sdk",
        };
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return "";
    }
}
