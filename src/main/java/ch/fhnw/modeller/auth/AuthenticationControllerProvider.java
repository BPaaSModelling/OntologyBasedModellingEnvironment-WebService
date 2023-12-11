package ch.fhnw.modeller.auth;

import com.auth0.AuthenticationController;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;

import javax.servlet.ServletConfig;
import java.io.UnsupportedEncodingException;

/**
 * The AuthenticationControllerProvider abstract class provides a static method to create an instance of AuthenticationController.
 */
public abstract class AuthenticationControllerProvider {
    private static JwkProvider jwkProvider = null;
    public static AuthenticationController getInstance(ServletConfig config) throws UnsupportedEncodingException {
        String domain = config.getServletContext().getInitParameter("com.auth0.domain");
        String clientId = config.getServletContext().getInitParameter("com.auth0.clientId");
        String clientSecret = config.getServletContext().getInitParameter("com.auth0.clientSecret");

        if (domain == null || clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("Missing domain, clientId, or clientSecret. Did you update src/main/webapp/WEB-INF/web.xml?");
        }

        // JwkProvider required for RS256 tokens. If using HS256, do not use.
        if (jwkProvider == null) {
            jwkProvider = new JwkProviderBuilder(domain).build();
        }
        return AuthenticationController.newBuilder(domain, clientId, clientSecret)
                .withJwkProvider(jwkProvider)
                .build();
    }


}
