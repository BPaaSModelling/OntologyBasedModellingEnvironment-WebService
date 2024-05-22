package ch.fhnw.modeller.webservice.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext cres) throws IOException {

        String origin = requestContext.getHeaderString("Origin");
        List<String> allowedOrigins = Arrays.asList("localhost", "herokuapp", "aoame", "api.aoame");

        if (origin != null) {
            boolean allowed = allowedOrigins.stream().anyMatch(origin::contains);
            if (allowed) {
                cres.getHeaders().add("Access-Control-Allow-Origin", origin);
            }
        }
        cres.getHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-User-Data");
        cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
        cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        cres.getHeaders().add("Access-Control-Max-Age", "1209600");

        // Handle preflight (OPTIONS) request
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            cres.setStatus(Response.Status.OK.getStatusCode());
        }
    }

}
