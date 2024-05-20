package ch.fhnw.modeller.webservice.filter;

import ch.fhnw.modeller.auth.AuthenticationControllerProvider;
import ch.fhnw.modeller.auth.SessionValidationServlet;
import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import com.auth0.AuthenticationController;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.google.gson.Gson;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CookieRequestFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = Logger.getLogger("JWTAuthFilter.class.getName()");
    private static final String AUTH0_DOMAIN = "https://dev-aoame.eu.auth0.com/";
    private static final String AUDIENCE = "https://aoame-webservice";
    private static final Gson gson = new Gson();

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        LOGGER.info("Authorization Header: " + authorizationHeader);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String token = authorizationHeader.substring("Bearer".length()).trim();
        try {
            DecodedJWT jwt = validateToken(token);
            SecurityContext securityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new JWTSecurityContext(jwt, securityContext.isSecure()));
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private DecodedJWT validateToken(String token) throws JwkException, IOException {
        DecodedJWT jwt = JWT.decode(token);

        JwkProvider provider = new UrlJwkProvider(new URL(AUTH0_DOMAIN + ".well-known/jwks.json"));
        Jwk jwk = provider.get(jwt.getKeyId());
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(AUTH0_DOMAIN)
                //.withAudience(AUDIENCE)
                .build();

        return verifier.verify(token);
    }
}

class JWTSecurityContext implements SecurityContext {

    private final DecodedJWT jwt;
    private final boolean secure;

    public JWTSecurityContext(DecodedJWT jwt, boolean secure) {
        this.jwt = jwt;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return () -> jwt.getSubject();
    }

    @Override
    public boolean isUserInRole(String role) {
        // Implement role-based authorization if needed
        return false;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }

}
