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
import java.util.Arrays;
import java.util.List;

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
     * Filters incoming requests and adds necessary headers for CORS support.
     * Also reads the access token and id token from the request.
     * Calls the next filter in the chain.
     *
     * @param request  the servlet request
     * @param response the servlet response
     * @param next     the next filter in the chain
     * @throws IOException      if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        final List<String> allowedOrigins = Arrays.asList("localhost", "herokuapp", "aoame", "api.aoame");

        if (origin != null) {
            for (String allowedOrigin : allowedOrigins) {
                if (origin.contains(allowedOrigin)) {
                    res.setHeader("Access-Control-Allow-Origin", origin);
                    break;
                }
            }

            //res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            res.setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
            res.setHeader("Access-Control-Allow-Credentials", "true");
        }

        //String accessToken = request.getParameter("accessToken");

        next.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
