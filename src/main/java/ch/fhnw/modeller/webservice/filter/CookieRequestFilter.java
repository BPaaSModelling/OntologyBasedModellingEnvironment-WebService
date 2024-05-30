package ch.fhnw.modeller.webservice.filter;

import ch.fhnw.modeller.auth.AuthenticationControllerProvider;
import ch.fhnw.modeller.auth.SessionValidationServlet;
import ch.fhnw.modeller.auth.UserService;

import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import com.auth0.AuthenticationController;
import com.auth0.json.mgmt.users.User;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.concurrent.*;
import static ch.fhnw.modeller.auth.SessionValidationServlet.getUserData;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CookieRequestFilter implements ContainerRequestFilter {
    private static final Logger LOGGER = Logger.getLogger("JWTAuthFilter.class.getName()");
    private static final String AUTH0_DOMAIN = "https://dev-aoame.eu.auth0.com/";
    private static final String AUDIENCE = "https://aoame-webservice";
//    private final TokenCache tokenCache = new TokenCache(); // Cache tokens for faster validation
    private static final Gson gson = new Gson();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        String userEmailHeader = requestContext.getHeaderString("X-User-Email");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String accessToken = authorizationHeader.substring("Bearer".length()).trim();
        try {
            // Decode user data
            User user = new User();
            user.setEmail(userEmailHeader);

            DecodedJWT jwt = validateToken(accessToken);
            SecurityContext securityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new JWTSecurityContext(jwt, securityContext.isSecure()));

//            User user = getUserData(userData);

            //userService.initializeUserGraph(user.getEmail());
            // Set Current user in the Ontology Manager for the upcoming request
            UserService userService = new UserService(user);
            requestContext.setProperty("userService", userService);
            OntologyManager.getInstance().setUserService(requestContext);
        } catch (Exception e) {
            LOGGER.severe("Error validating token: " + e.getMessage());
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

//    private User getUserData(String idToken) throws IOException, JwkException {
//        final DecodedJWT jwt = validateToken(idToken);
//        // Create User object
//        User user = new User();
//
//        user.setSub(jwt.getSubject());
//        user.setAud(String.valueOf(jwt.getAudience()));
//        user.setEmailVerified(jwt.getClaim("email_verified").asBoolean());
//        user.setUpdatedAt(jwt.getClaim("updated_at").asString());
//        user.setIss(jwt.getIssuer());
//        user.setNickname(jwt.getClaim("nickname").asString());
//        user.setName(jwt.getClaim("name").asString());
////        user.setExp(jwt.getExpiresAt());
////        user.setIat(jwt.getIssuedAt());
//        user.setEmail(jwt.getClaim("email").asString());
//        user.setSid(jwt.getId());
//        user.setSid(jwt.getClaim("sid").asString());
//
//        return user;
//    }

    private DecodedJWT validateToken(String token) throws JwkException, IOException {
//        DecodedJWT jwt = tokenCache.get(token);
//        if (jwt != null) {
//            return jwt;
//        }

        DecodedJWT jwt = JWT.decode(token);

        JwkProvider provider = new UrlJwkProvider(new URL(AUTH0_DOMAIN + ".well-known/jwks.json"));
        Jwk jwk = provider.get(jwt.getKeyId());
        RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(AUTH0_DOMAIN)
                //.withAudience(AUDIENCE)
                .build();

        jwt = verifier.verify(token);
//        tokenCache.put(token, jwt);

        return  jwt;
    }
}

class TokenCache {
    private final ConcurrentHashMap<String, DecodedJWT> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public DecodedJWT get(String token) {
        return cache.get(token);
    }

    public void put(String token, DecodedJWT jwt) {
        Future<?> oldTask = (Future<?>) cache.put(token, jwt);
        if (oldTask != null) {
            oldTask.cancel(false);
        }

        Runnable expirationTask = () -> cache.remove(token);
        executorService.schedule(expirationTask, 1, TimeUnit.HOURS);
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
