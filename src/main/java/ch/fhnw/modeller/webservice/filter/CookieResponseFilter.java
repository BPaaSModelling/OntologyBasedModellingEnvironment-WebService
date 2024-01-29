package ch.fhnw.modeller.webservice.filter;

import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CookieResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        // This filter is executed after the request is processed

    }
}
