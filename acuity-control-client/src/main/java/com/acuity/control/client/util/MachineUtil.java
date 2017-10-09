package com.acuity.control.client.util;


import com.acuity.db.domain.vertex.impl.machine.MachineUpdate;
import com.sun.management.OperatingSystemMXBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Zachary Herridge on 8/14/2017.
 */
public class MachineUtil {


    /**
     * Sun property pointing the main class and its arguments.
     * Might not be defined on non Hotspot VM implementations.
     */
    public static final String SUN_JAVA_COMMAND = "sun.java.command";

    public static MachineUpdate buildMachineState() {
        MachineUpdate machineUpdate = new MachineUpdate();

        HashMap<String, Object> properties = new HashMap<>();
        System.getProperties().keySet().forEach(key -> properties.put(String.valueOf(key), System.getProperty(String.valueOf(key))));
        machineUpdate.setProperties(properties);
        machineUpdate.setMacAddress(getMacAddress());

        return machineUpdate;
    }

    public static String getMacAddress() {
        try {
            InetAddress localIP = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(localIP);
            byte[] macAddress = network.getHardwareAddress();
            StringBuilder macAddressStringBuilder = new StringBuilder();
            for (int i = 0; i < macAddress.length; i++) {
                macAddressStringBuilder.append(String.format("%02X%s", macAddress[i], (i < macAddress.length - 1) ? "-" : ""));
            }
            return macAddressStringBuilder.toString();
        } catch (Throwable ignored) {
        }
        return "Unknown";
    }

    public static float getCPUUsage() {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        long prevUpTime = runtimeMXBean.getUptime();
        long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
        double cpuUsage;
        try {
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;
        return Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
    }

    public static void download(String url, String path) throws IOException {
        URL download = new URL(url);
        try (InputStream in = download.openStream()) {
            Files.copy(in, new File(path).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void restartApplication(Runnable runBeforeRestart) throws IOException {
        try {
            String java = System.getProperty("java.home") + "/bin/java";

            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {

                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }

            final StringBuffer cmd = new StringBuffer("\"" + java + "\" " + vmArgsOneLine);

            String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");

            if (mainCommand[0].endsWith(".jar")) {

                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {

                cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
            }

            for (int i = 1; i < mainCommand.length; i++) {
                cmd.append(" ");
                cmd.append(mainCommand[i]);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Runtime.getRuntime().exec(cmd.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            if (runBeforeRestart != null) {
                runBeforeRestart.run();
            }

            System.exit(0);
        } catch (Exception e) {
            throw new IOException("Error while trying to restart the application", e);
        }
    }
}
