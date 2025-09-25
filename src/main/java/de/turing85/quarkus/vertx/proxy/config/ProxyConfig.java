package de.turing85.quarkus.vertx.proxy.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.vertx-proxy")
public interface ProxyConfig {
  int DEFAULT_PORT = 8880;
  int DEFAULT_TEST_PORT = 8881;
  String DEFAULT_HOST_NAME_FROM_APP_NAME = "${quarkus.application.name}";

  @WithName("http.port")
  @WithDefault("" + DEFAULT_PORT)
  int httpPort();

  @WithName("http.test-port")
  @WithDefault("" + DEFAULT_TEST_PORT)
  @SuppressWarnings("unused")
  int httpTestPort();

  @WithDefault(DEFAULT_HOST_NAME_FROM_APP_NAME)
  String hostName();
}
