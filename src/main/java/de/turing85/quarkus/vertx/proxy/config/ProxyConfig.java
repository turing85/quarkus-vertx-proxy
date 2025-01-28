package de.turing85.quarkus.vertx.proxy.config;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.vertx-proxy")
@ConfigRoot
public interface ProxyConfig {
  int DEFAULT_PORT = 8880;
  int DEFAULT_TEST_PORT = 8881;

  @WithName("http.port")
  int httpPort();

  @WithName("http.test-port")
  @WithDefault("" + DEFAULT_TEST_PORT)
  @SuppressWarnings("unused")
  int httpTestPort();
}
