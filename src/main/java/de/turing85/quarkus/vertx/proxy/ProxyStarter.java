package de.turing85.quarkus.vertx.proxy;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Singleton;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.HttpServerStart;
import io.smallrye.common.cpu.ProcessorInfo;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

@Singleton
public class ProxyStarter {
  void observe(@ObservesAsync final HttpServerStart ignored,
      @SuppressWarnings("CdiInjectionPointsInspection") final Vertx vertx,
      final ProxyConfig proxyConfig, final LaunchMode launchMode,
      final VertxConfiguration vertxConfiguration) {
    // @formatter:off
    vertx.deployVerticle(() -> new ProxyVerticle(
        proxyConfig.httpPort(), launchMode),
        new DeploymentOptions().setInstances(vertxConfiguration.eventLoopsPoolSize()
            .orElseGet(ProxyStarter::calculateDefaultIOThreads)));
    // @formatter:on
  }

  private static int calculateDefaultIOThreads() {
    // we only allow one event loop per 10mb of ram at the most
    // it's hard to say what this number should be, but it is also obvious
    // that for constrained environments we don't want a lot of event loops
    // lets start with 10mb and adjust as needed
    // We used to recommend a default of twice the number of cores,
    // but more recent developments seem to suggest matching the number of cores 1:1
    // being a more reasonable default. It also saves memory.
    final int recommended = ProcessorInfo.availableProcessors();
    final long memInMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
    final long maxAllowed = memInMb / 10;
    return (int) Math.clamp(recommended, 2, maxAllowed);
  }
}
