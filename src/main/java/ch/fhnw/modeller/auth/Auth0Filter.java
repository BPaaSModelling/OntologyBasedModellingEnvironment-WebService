package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.webservice.filter.CORSFilter;
import com.auth0.AuthenticationController;
import com.auth0.SessionUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.interfaces.RSAPublicKey;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.RSAKeyProvider;

/**
 * Filter class to check if a valid session exists. This will be true if the User Id is present.
 */
@WebFilter(urlPatterns = "/*")
public class Auth0Filter implements Filter{
    private AuthenticationController authenticationController;
    private JwkProvider jwkProvider;
    private String domain;
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.domain = servletContext.getInitParameter("com.auth0.domain");

        if (domain == null) {
            throw new ServletException("Domain parameter is missing in the configuration");
        }

        this.jwkProvider = new JwkProviderBuilder(domain).build();

    }

    /**
     * Perform filter check on this request - verify the User Id is present.
     *
     * @param request  the received request
     * @param response the response to send
     * @param next     the next filter chain
     **/
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        res.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");

        //String accessToken = request.getParameter("accessToken");

        // This is an example how to read the token from the header
        String accessToken = req.getParameter("accessToken");
        String idToken = req.getParameter( "idToken");

        next.doFilter(request, response);
    }

    public boolean isValidToken(String accessToken){
//        try {
//            DecodedJWT jwt = JWT.decode(accessToken); // Decode token to get the Key ID (kid)
//            Jwk jwk = jwkProvider.get(jwt.getKeyId()); // Get the public key from Auth0 using JWK Provider
//
//            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
//            JWTVerifier verifier = JWT.require(algorithm)
//                    .withIssuer("https://"+domain+"/")
//                    .build(); //Reusable verifier instance
//            verifier.verify(accessToken);
//        } catch (JWTVerificationException | JwkException exception){
//            //Invalid signature/claims
//            return false;
//        }
        return true;
    }

    @Override
    public void destroy() {
    }

}
