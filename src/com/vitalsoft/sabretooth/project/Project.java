package com.vitalsoft.sabretooth.project;

import com.vitalsoft.sabretooth.config.SabreConf;
import java.io.*;
import java.util.*;

public class Project {
    private File rootDir;
    private SabreConf conf;
    private String name;

    public Project(File rootDir) throws IOException {
        this.rootDir = rootDir;
        this.name = rootDir.getName();
        File confFile = new File(rootDir, ".sabreconf");
        if (confFile.exists()) {
            conf = new SabreConf(confFile);
            String confName = conf.get(SabreConf.KEY_PROJECT_NAME);
            if (!confName.isEmpty()) name = confName;
        } else {
            conf = new SabreConf();
            conf.set(SabreConf.KEY_PROJECT_NAME, name);
        }
    }

    public static Project create(File parentDir, String projectName, String packageName,
                                  int minSdk, int targetSdk, int compileSdk,
                                  String gradlePath, String jdkPath, String androidJar,
                                  String projectType) throws IOException {
        File projectDir = new File(parentDir, projectName);
        if (!projectDir.exists()) projectDir.mkdirs();

        // Create project structure
        if ("gradle".equalsIgnoreCase(projectType)) {
            createGradleStructure(projectDir, projectName, packageName, minSdk, targetSdk, compileSdk);
        } else {
            createEclipseStructure(projectDir, projectName, packageName, minSdk, targetSdk, compileSdk);
        }

        // Create .sabreconf
        SabreConf conf = new SabreConf();
        conf.set(SabreConf.KEY_PROJECT_NAME, projectName);
        conf.set(SabreConf.KEY_PACKAGE_NAME, packageName);
        conf.set(SabreConf.KEY_MIN_SDK, String.valueOf(minSdk));
        conf.set(SabreConf.KEY_TARGET_SDK, String.valueOf(targetSdk));
        conf.set(SabreConf.KEY_COMPILE_SDK, String.valueOf(compileSdk));
        conf.set(SabreConf.KEY_GRADLE_PATH, gradlePath != null ? gradlePath : "");
        conf.set(SabreConf.KEY_JDK_PATH, jdkPath != null ? jdkPath : "");
        conf.set(SabreConf.KEY_ANDROID_JAR, androidJar != null ? androidJar : "");
        conf.set(SabreConf.KEY_PROJECT_TYPE, projectType);
        conf.set(SabreConf.KEY_MAIN_ACTIVITY, "MainActivity");
        conf.save(new File(projectDir, ".sabreconf"));

        return new Project(projectDir);
    }

    private static void createGradleStructure(File root, String name, String pkg,
                                               int minSdk, int targetSdk, int compileSdk) throws IOException {
        String pkgPath = pkg.replace('.', File.separatorChar);
        File javaDir = new File(root, "app/src/main/java/" + pkgPath);
        File resDir  = new File(root, "app/src/main/res/layout");
        File valDir  = new File(root, "app/src/main/res/values");
        javaDir.mkdirs();
        resDir.mkdirs();
        valDir.mkdirs();
        new File(root, "app/src/main/assets").mkdirs();

        // settings.gradle
        writeFile(new File(root, "settings.gradle"),
            "rootProject.name = \"" + name + "\"\n" +
            "include ':app'\n");

        // build.gradle (root)
        writeFile(new File(root, "build.gradle"),
            "buildscript {\n" +
            "    repositories {\n" +
            "        google()\n" +
            "        mavenCentral()\n" +
            "    }\n" +
            "    dependencies {\n" +
            "        classpath 'com.android.tools.build:gradle:8.2.0'\n" +
            "    }\n" +
            "}\n\n" +
            "allprojects {\n" +
            "    repositories {\n" +
            "        google()\n" +
            "        mavenCentral()\n" +
            "    }\n" +
            "}\n");

        // app/build.gradle
        writeFile(new File(root, "app/build.gradle"),
            "apply plugin: 'com.android.application'\n\n" +
            "android {\n" +
            "    compileSdk " + compileSdk + "\n" +
            "    defaultConfig {\n" +
            "        applicationId \"" + pkg + "\"\n" +
            "        minSdk " + minSdk + "\n" +
            "        targetSdk " + targetSdk + "\n" +
            "        versionCode 1\n" +
            "        versionName \"1.0\"\n" +
            "    }\n" +
            "    buildTypes {\n" +
            "        release {\n" +
            "            minifyEnabled false\n" +
            "        }\n" +
            "    }\n" +
            "}\n\n" +
            "dependencies {\n" +
            "    implementation 'androidx.appcompat:appcompat:1.6.1'\n" +
            "    implementation 'com.google.android.material:material:1.11.0'\n" +
            "}\n");

        // AndroidManifest.xml
        writeFile(new File(root, "app/src/main/AndroidManifest.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    package=\"" + pkg + "\">\n\n" +
            "    <application\n" +
            "        android:allowBackup=\"true\"\n" +
            "        android:icon=\"@mipmap/ic_launcher\"\n" +
            "        android:label=\"@string/app_name\"\n" +
            "        android:roundIcon=\"@mipmap/ic_launcher_round\"\n" +
            "        android:supportsRtl=\"true\"\n" +
            "        android:theme=\"@style/Theme.AppCompat.Light.DarkActionBar\">\n" +
            "        <activity\n" +
            "            android:name=\".MainActivity\"\n" +
            "            android:exported=\"true\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n" +
            "    </application>\n\n" +
            "</manifest>\n");

        // MainActivity.java
        writeFile(new File(javaDir, "MainActivity.java"),
            "package " + pkg + ";\n\n" +
            "import android.app.Activity;\n" +
            "import android.os.Bundle;\n\n" +
            "public class MainActivity extends Activity {\n\n" +
            "    @Override\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "        setContentView(R.layout.activity_main);\n" +
            "    }\n" +
            "}\n");

        // activity_main.xml
        writeFile(new File(resDir, "activity_main.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:gravity=\"center\"\n" +
            "    android:orientation=\"vertical\">\n\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"Hello World!\"\n" +
            "        android:textSize=\"24sp\" />\n\n" +
            "</LinearLayout>\n");

        // strings.xml
        writeFile(new File(valDir, "strings.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <string name=\"app_name\">" + name + "</string>\n" +
            "</resources>\n");

        // gradle wrapper
        File wrapperDir = new File(root, "gradle/wrapper");
        wrapperDir.mkdirs();
        writeFile(new File(wrapperDir, "gradle-wrapper.properties"),
            "distributionBase=GRADLE_USER_HOME\n" +
            "distributionPath=wrapper/dists\n" +
            "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.2-bin.zip\n" +
            "zipStoreBase=GRADLE_USER_HOME\n" +
            "zipStorePath=wrapper/dists\n");

        // .gitignore
        writeFile(new File(root, ".gitignore"),
            "*.iml\n.gradle\n/local.properties\n/.idea\n/build\n/captures\n.externalNativeBuild\n.cxx\n");
    }

    private static void createEclipseStructure(File root, String name, String pkg,
                                                int minSdk, int targetSdk, int compileSdk) throws IOException {
        String pkgPath = pkg.replace('.', File.separatorChar);
        new File(root, "src/" + pkgPath).mkdirs();
        new File(root, "res/layout").mkdirs();
        new File(root, "res/values").mkdirs();
        new File(root, "bin").mkdirs();
        new File(root, "libs").mkdirs();

        // project.properties
        writeFile(new File(root, "project.properties"),
            "target=android-" + compileSdk + "\n");

        // .classpath
        writeFile(new File(root, ".classpath"),
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<classpath>\n" +
            "    <classpathentry kind=\"src\" path=\"src\"/>\n" +
            "    <classpathentry kind=\"src\" path=\"gen\"/>\n" +
            "    <classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.ANDROID_FRAMEWORK\"/>\n" +
            "    <classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.LIBRARIES\"/>\n" +
            "    <classpathentry kind=\"con\" path=\"com.android.ide.eclipse.adt.DEPENDENCIES\"/>\n" +
            "    <classpathentry kind=\"output\" path=\"bin/classes\"/>\n" +
            "</classpath>\n");

        // AndroidManifest.xml
        writeFile(new File(root, "AndroidManifest.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    package=\"" + pkg + "\"\n" +
            "    android:versionCode=\"1\"\n" +
            "    android:versionName=\"1.0\">\n\n" +
            "    <uses-sdk android:minSdkVersion=\"" + minSdk + "\" android:targetSdkVersion=\"" + targetSdk + "\" />\n\n" +
            "    <application\n" +
            "        android:icon=\"@drawable/ic_launcher\"\n" +
            "        android:label=\"@string/app_name\">\n" +
            "        <activity android:name=\".MainActivity\" android:label=\"@string/app_name\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n" +
            "    </application>\n" +
            "</manifest>\n");

        writeFile(new File(root, "src/" + pkgPath + "/MainActivity.java"),
            "package " + pkg + ";\n\n" +
            "import android.app.Activity;\n" +
            "import android.os.Bundle;\n\n" +
            "public class MainActivity extends Activity {\n\n" +
            "    @Override\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "        setContentView(R.layout.activity_main);\n" +
            "    }\n" +
            "}\n");

        writeFile(new File(root, "res/layout/activity_main.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:gravity=\"center\"\n" +
            "    android:orientation=\"vertical\">\n\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"Hello World!\"\n" +
            "        android:textSize=\"24sp\" />\n\n" +
            "</LinearLayout>\n");

        writeFile(new File(root, "res/values/strings.xml"),
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <string name=\"app_name\">" + name + "</string>\n" +
            "</resources>\n");
    }

    private static void writeFile(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.print(content);
        }
    }

    public File getRootDir() { return rootDir; }
    public SabreConf getConf() { return conf; }
    public String getName() { return name; }

    public void saveConf() throws IOException {
        conf.save(new File(rootDir, ".sabreconf"));
    }

    @Override
    public String toString() { return name; }
}
