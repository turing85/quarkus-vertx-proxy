package de.turing85.quarkus.vertx.proxy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Singleton;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.HttpServerStart;
import io.smallrye.common.cpu.ProcessorInfo;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.eclipse.microprofile.config.ConfigProvider;

@Singleton
class ProxyStarter {
  void observe(@ObservesAsync final HttpServerStart ignored, final LaunchMode launchMode,
      @SuppressWarnings("CdiInjectionPointsInspection") final Vertx vertx,
      final VertxConfiguration vertxConfiguration, final ProxyConfig proxyConfig,
      final InjectableInstance<ProxyInterceptorProvider> providers) {
    final int httpPort = calculateHttpPort(launchMode);
    final List<ProxyInterceptorProvider> prioritySortedProviders = sortByPriority(providers);
    // @formatter:off
    final int proxyHttpPort = proxyConfig.httpPort();
    deployProxyVerticle(vertx, vertxConfiguration, proxyHttpPort, httpPort, prioritySortedProviders)
        .onSuccess(unused -> Log.infof(
            "vert.x proxy started on  http://localhost:%d, forwarding to http://localhost:%d",
            proxyHttpPort, httpPort));
    // @formatter:on
  }

  private static List<ProxyInterceptorProvider> sortByPriority(
      final InjectableInstance<ProxyInterceptorProvider> providers) {
    // @formatter:off
    return StreamSupport.stream(providers.handles().spliterator(), false)
        .sorted(Comparator.comparingInt(ProxyStarter::extractPriority))
        .map(InstanceHandle::get)
        .toList();
    // @formatter:on
  }

  private static Future<String> deployProxyVerticle(final Vertx vertx,
      final VertxConfiguration vertxConfiguration, final int proxyHttpPort, final int httpPort,
      final List<ProxyInterceptorProvider> prioritySortedProviders) {
    // @formatter:off
    return vertx.deployVerticle(
        () -> new ProxyVerticle(proxyHttpPort, httpPort, prioritySortedProviders),
        new DeploymentOptions().setInstances(
            vertxConfiguration.eventLoopsPoolSize()
                .orElseGet(ProxyStarter::calculateDefaultIOThreads)));
    // @formatter:on
  }

  private static int calculateHttpPort(LaunchMode launchMode) {
    final String portPropertyName = launchMode == LaunchMode.TEST ? "test-port" : "port";
    return ConfigProvider.getConfig().getValue("quarkus.http.%s".formatted(portPropertyName),
        Integer.class);
  }

  private static int extractPriority(final InstanceHandle<ProxyInterceptorProvider> handle) {
    if (handle.getBean().getPriority() != 0) {
      return handle.getBean().getPriority();
    }
    // @formatter:off
    return Optional.ofNullable(handle.get().getClass().getAnnotation(Priority.class))
        .map(Priority::value)
        .orElse(ProxyInterceptorProvider.DEFAULT_PRIORITY);
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
