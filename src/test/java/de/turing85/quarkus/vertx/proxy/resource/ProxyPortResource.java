package de.turing85.quarkus.vertx.proxy.resource;

import java.net.ServerSocket;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;

public class ProxyPortResource implements QuarkusTestResourceLifecycleManager {
  private static Integer actualPort;

  @Override
  public Map<String, String> start() {
    final int configuredTestPort = ConfigProvider.getConfig()
        .getOptionalValue("quarkus.vertx-proxy.http.test-port", Integer.class)
        .orElse(ProxyConfig.DEFAULT_PORT);
    if (configuredTestPort < 0) {
      actualPort = findFreePort();
    } else {
      actualPort = configuredTestPort;
    }
    String actualPortAsString = Integer.toString(actualPort);
    return Map.of("quarkus.vertx-proxy.http.port", actualPortAsString,
        "quarkus.test.container.additional-exposed-ports.%d".formatted(actualPort),
        actualPortAsString);
  }

  private static int findFreePort() {
    int tries = 0;
    final Random random = new Random();
    while (true) {
      if (tries > 1_000) {
        Log.error("Cannot find a free port");
        throw new RuntimeException("Cannot find a free port");
      }
      int candidate = random.nextInt(1000, 65_536);
      try {
        new ServerSocket(candidate).close();
        return candidate;
      } catch (Exception e) {
        ++tries;
      }
    }
  }

  @Override
  public void stop() {
    actualPort = null;
  }

  public static String proxyUrl() {
    // @formatter:off
    return Optional.ofNullable(actualPort)
        .map("http://localhost:%d"::formatted)
        .orElseThrow(() -> new IllegalStateException("Proxy has not yet been initialized"));
    // @formatter:on
  }
}
