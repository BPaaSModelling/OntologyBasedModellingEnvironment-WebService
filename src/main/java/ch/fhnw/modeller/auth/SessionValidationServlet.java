package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.exception.NoResultsException;
import com.auth0.AuthenticationController;
import com.auth0.SessionUtils;
import com.auth0.Tokens;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.auth0.exception.IdTokenValidationException;
import com.auth0.json.mgmt.Token;
import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.google.gson.Gson;
import org.apache.jena.base.Sys;
import org.json.JSONObject;

/**
 * The SessionValidationServlet class is a servlet responsible for validating user sessions and providing user data.
 * It handles accessToken and idToken cookies, decodes and validates them.
 */
@WebServlet("/auth")
public class SessionValidationServlet extends HttpServlet {
    @Context
    private ContainerRequestContext crc;

    private  UserService userService;
    private static AuthenticationController authenticationController;
    public static String domain;

    private Gson gson = new Gson();
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            authenticationController = AuthenticationControllerProvider.getInstance(config);
            domain = config.getServletContext().getInitParameter("com.auth0.domain");
        } catch (UnsupportedEncodingException e) {
            throw new ServletException("Couldn't create the AuthenticationController instance. Check the configuration.", e);
        }
    }
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        String accessToken = null; //= req.getParameter("accessToken");
        String idToken = null; //= req.getParameter( "idToken");
        User user = null;

        try {
            Cookie[] cookies = req.getCookies();
            res.setContentType("application/json");
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("accessToken")) {
                        accessToken = cookie.getValue();
                    } else if (cookie.getName().equals("idToken")) {
                        idToken = cookie.getValue();
                    }
                    System.out.println("Cookie " + cookie.getName() +": "+ cookie.getValue());
                }
            }

            // Development environment auto authentication if user haven't logged in
            if ("true".equals(System.getenv("DEV_ENV"))) {
                DevEnv.setTestUser(req, res);
                accessToken = DevEnv.getAccessToken();
                idToken = DevEnv.getIdToken();
                System.out.println("Test User is set for " + System.getenv("USER_EMAIL"));
            }

            if (accessToken != null && idToken != null) {
                // Validate the idToken and Get and Set the User
                user = getUserData(idToken);
                // Initialize User Service and store it in the session
                userService = new UserService(user);
                //Initialize Graph upon login (create if doesn't exist and duplicate data from default graph)
                userService.initializeUserGraph(userService.getUserGraphUri());
            } else {
                throw new InvalidParameterException("Tokens cannot be null or empty. User will be redirected to login page to receive new tokens.");
            }

            Gson gson = new Gson();
            String payload = gson.toJson(user);
            //addTokenCookies(accessToken, idToken, user, res);
            res.setHeader("Authorization", accessToken);
            res.getWriter().write(payload);

            res.setStatus(HttpServletResponse.SC_OK);
        } catch (NoResultsException e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.getWriter().write("Jena Fuseki Server: " + e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Unauthorized: " + e.getMessage());
        }
    }

    public static User getUserData(String idToken) {
            final DecodedJWT jwt = validateToken(idToken);
            // Create User object
            User user = new User();

            user.setSub(jwt.getSubject());
            user.setAud(jwt.getAudience());
            user.setEmail_verified(jwt.getClaim("email_verified").asBoolean());
            user.setUpdated_at(jwt.getClaim("updated_at").asString());
            user.setIss(jwt.getIssuedAt());
            user.setNickname(jwt.getClaim("nickname").asString());
            user.setName(jwt.getClaim("name").asString());
            user.setExp(jwt.getExpiresAt());
            user.setIat(jwt.getIssuedAt());
            user.setEmail(jwt.getClaim("email").asString());
            user.setSid(jwt.getId());
            user.setSid(jwt.getClaim("sid").asString());

            return user;
    }

    public static DecodedJWT validateToken(String token) {
        // This can involve checking the signature, expiry, etc.
        try {
            if (token == null || token.isEmpty())
                throw new IdTokenValidationException("Token cannot be null or empty. User will be redirected to login page");

            final DecodedJWT jwt = JWT.decode(token);
            //JwkProvider jwkProvider =  AuthenticationControllerProvider.getJwkProvider();
            if (!jwt.getIssuer().contains(domain)) {
                throw new IdTokenValidationException(String.format("Unknown Issuer %s", jwt.getIssuer()));
            }
            RSAPublicKey publicKey = loadPublicKey(jwt);

            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(jwt.getIssuer())
                    .build();

            verifier.verify(token);
            return jwt;
        } catch (TokenExpiredException e) {
            Logger.getLogger(SessionValidationServlet.class.getName()).log(Level.INFO, "JWT Token expired, a new Token will be created upon next login. ");
            throw new TokenExpiredException("JWT Token expired, a new Token will be created upon next login. ");
        } catch (Exception e) {
            //Invalid token
            Logger.getLogger(SessionValidationServlet.class.getName()).log(Level.WARNING, "JWT validation failed: ", e);
            throw new IdTokenValidationException("JWT validation failed: "+e.getMessage());
        }
    }

    private static RSAPublicKey loadPublicKey(DecodedJWT token) throws JwkException {
        JwkProvider provider = AuthenticationControllerProvider.getJwkProvider(); //UrlJwkProvider(("https://" + domain + "/"));

        return (RSAPublicKey) provider.get(token.getKeyId()).getPublicKey();
    }
}


