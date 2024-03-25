package ch.fhnw.modeller.webservice.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

   @Override
   public void filter(final ContainerRequestContext requestContext,
                      final ContainerResponseContext cres) throws IOException {

      String origin = requestContext.getHeaderString("Origin");
      List<String> allowedOrigins = Arrays.asList("localhost", "herokuapp", "aoame", "api.aoame");

      if (origin != null) {
         for (String allowedOrigin : allowedOrigins) {
            if (origin.contains(allowedOrigin)) {
               cres.getHeaders().add("Access-Control-Allow-Origin", origin);
               break;
            }
         }
      }
      cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
      cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
      cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
      cres.getHeaders().add("Access-Control-Max-Age", "1209600");
   }

}
