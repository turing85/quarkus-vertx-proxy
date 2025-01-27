package de.turing85.quarkus.endpoint;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;

import io.smallrye.mutiny.Uni;

@Path("hello")
public class Endpoint {
  @GET
  public Uni<Response> uniResponse(@QueryParam("eTag") String eTag) {
    final Response.ResponseBuilder response = Response.ok("Hello");
    if (eTag != null && !eTag.isEmpty()) {
      response.tag(new EntityTag(eTag));
    }
    return Uni.createFrom().item(response.build());
  }
}
