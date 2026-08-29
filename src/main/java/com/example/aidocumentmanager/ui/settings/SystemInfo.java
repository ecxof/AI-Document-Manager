package com.example.aidocumentmanager.ui.settings;

import com.example.aidocumentmanager.common.ByteFormat;

/**
 * The runtime and memory report shown on the Settings panel.
 */
public class SystemInfo {

    /** Snapshot of the JVM, the OS, and current memory use. */
    public static String describe() {
        StringBuilder sysInfo = new StringBuilder();
        sysInfo.append("System Information:\n\n");
        sysInfo.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sysInfo.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sysInfo.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        sysInfo.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sysInfo.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
        sysInfo.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        sysInfo.append("Working Directory: ").append(System.getProperty("user.dir")).append("\n");

        // Memory information
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        sysInfo.append("\nMemory Usage:\n");
        sysInfo.append("• Used: ").append(ByteFormat.format(usedMemory)).append("\n");
        sysInfo.append("• Free: ").append(ByteFormat.format(freeMemory)).append("\n");
        sysInfo.append("• Total: ").append(ByteFormat.format(totalMemory)).append("\n");
        sysInfo.append("• Max: ").append(ByteFormat.format(maxMemory)).append("\n");

        return sysInfo.toString();
    }
}
