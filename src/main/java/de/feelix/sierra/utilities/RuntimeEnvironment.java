package de.feelix.sierra.utilities;

import java.lang.management.ManagementFactory;
import java.util.List;

public class RuntimeEnvironment {


    public static boolean isValid() {
        List<String> jvmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String[] criticalParams = {
            "-javaagent",
            "-agentlib",
            "-Djava.security.manager",
            "-Djava.security.policy",
            "-Xbootclasspath",
            "-classpath",
            "-Djava.class.path",
            "-Dsun.java.command",
            "-Djava.rmi.server.hostname",
            "-Djavax.net.ssl.trustStore",
            "-Dsun.net.client.defaultConnectTimeout",
            "-Dsun.net.client.defaultReadTimeout",
            "-Djava.io.tmpdir",
            "-Djava.util.logging.config.file",
            "-Djava.awt.headless",
            "-Dsun.security.ssl.allowUnsafeRenegotiation",
            "-Dsun.net.http.allowRestrictedHeaders",
            "-Dsun.net.http.allowRestrictedMethods",
            "-Dsun.net.http.retryPost",
            "-Dsun.net.http.redirectProxy",
            "-Dcom.sun.management.jmxremote",
            "-Dcom.sun.management.jmxremote.port",
            "-Dcom.sun.management.jmxremote.authenticate",
            "-Dcom.sun.management.jmxremote.ssl",
            "-Dcom.sun.management.jmxremote.password.file",
            "-Dcom.sun.management.jmxremote.access.file",
            "-Dcom.sun.management.jmxremote.ssl.need.client.auth",
            "-Dcom.sun.management.jmxremote.local.only",
            "-Dsun.net.spi.nameservice.provider.1",
            "-Dsun.net.spi.nameservice.provider.2",
            "-Dsun.net.spi.nameservice.provider.3",
            "-Djava.util.prefs.PreferencesFactory"
        };

        for (String arg : jvmArguments) {
            for (String criticalParam : criticalParams) {
                if (arg.startsWith(criticalParam)) {
                    return false;
                }
            }
        }

        return true;
    }
}
