package de.turing85.quarkus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

@Path(HelloResource.PATH)
public class HelloResource {
  public static final String PATH = "hello";

  @GET
  public Uni<Response> uniResponse(@QueryParam("eTag") final String eTag) {
    final Response.ResponseBuilder response = Response.ok("Hello");
    if (eTag != null && !eTag.isEmpty()) {
      response.tag(new EntityTag(eTag));
    }
    return Uni.createFrom().item(response.build());
  }
}
