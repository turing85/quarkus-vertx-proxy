package de.turing85.quarkus.resource;

import java.net.URI;
import java.util.Optional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;
import org.jspecify.annotations.Nullable;

@Path(HelloResource.PATH)
@Produces(MediaType.TEXT_PLAIN)
public class HelloResource {
  public static final String PATH = "hello";

  @POST
  public Uni<Response> post() {
    return Uni.createFrom().item(Response.created(URI.create(PATH)).entity(PATH).build());
  }

  @GET
  public Uni<Response> get(@QueryParam("eTag") @Nullable final String eTag) {
    final Response.ResponseBuilder response = Response.ok(PATH);
    if (!Optional.ofNullable(eTag).orElse("").isEmpty()) {
      response.tag(new EntityTag(eTag));
    }
    return Uni.createFrom().item(response.build());
  }
}
