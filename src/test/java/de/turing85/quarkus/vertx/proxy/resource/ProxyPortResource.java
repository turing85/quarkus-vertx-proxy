package de.turing85.quarkus.vertx.proxy.resource;

import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jspecify.annotations.Nullable;

public class ProxyPortResource implements QuarkusTestResourceLifecycleManager {
  @Nullable
  private static Integer actualPort;

  @Override
  public Map<String, String> start() {
    final int configuredTestPort = ConfigProvider.getConfig()
        .getOptionalValue("quarkus.vertx-proxy.http.test-port", Integer.class)
        .orElse(ProxyConfig.DEFAULT_TEST_PORT);
    actualPort = getActualPort(configuredTestPort);
    final String actualPortAsString = Integer.toString(actualPort);
    return Map.of("quarkus.vertx-proxy.http.port", actualPortAsString,
        "quarkus.test.container.additional-exposed-ports.%d".formatted(actualPort),
        actualPortAsString);
  }

  private static int getActualPort(final int configuredTestPort) {
    if (configuredTestPort < 0) {
      return getRandomPort();
    } else {
      return configuredTestPort;
    }
  }

  private static int getRandomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (Exception e) {
      throw new RuntimeException("Cannot find a free port");
    }
  }

  @Override
  public void stop() {
    actualPort = null;
  }

  @Override
  public void inject(final TestInjector testInjector) {
    // @formatter:off
    testInjector.injectIntoFields(
        Optional.ofNullable(actualPort)
            .map(port -> URI.create("http://localhost:%d".formatted(port)))
            .orElseThrow(() -> new IllegalStateException("Proxy has not yet been initialized")),
        new TestInjector.AnnotatedAndMatchesType(InjectProxy.class, URI.class));
    // @formatter:on
  }
}
