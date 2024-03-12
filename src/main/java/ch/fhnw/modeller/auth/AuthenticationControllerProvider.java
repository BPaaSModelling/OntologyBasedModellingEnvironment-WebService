package ch.fhnw.modeller.auth;

import com.auth0.AuthenticationController;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import lombok.Getter;

import javax.servlet.ServletConfig;
import java.io.UnsupportedEncodingException;

/**
 * The AuthenticationControllerProvider abstract class provides a static method to create an instance of AuthenticationController.
 */
public abstract class AuthenticationControllerProvider {
    @Getter
    private static JwkProvider jwkProvider;

    public static String domain;
    public static String clientId;
    public static String clientSecret;
    public static AuthenticationController getInstance(ServletConfig config) throws UnsupportedEncodingException {
        domain = config.getServletContext().getInitParameter("com.auth0.domain");
        clientId = config.getServletContext().getInitParameter("com.auth0.clientId");
         clientSecret = config.getServletContext().getInitParameter("com.auth0.clientSecret");

        if (domain == null || clientId == null || clientSecret == null) {
            throw new IllegalArgumentException("Missing domain, clientId, or clientSecret. Did you update src/main/webapp/WEB-INF/web.xml?");
        }

        // JwkProvider required for RS256 tokens. If using HS256, do not use.

        jwkProvider = new JwkProviderBuilder(domain).build();

        return AuthenticationController.newBuilder(domain, clientId, clientSecret)
                .withJwkProvider(jwkProvider)
                .build();
    }


}
