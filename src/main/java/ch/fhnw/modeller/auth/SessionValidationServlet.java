package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.google.gson.Gson;
import org.json.JSONObject;

@WebServlet("/auth")
public class SessionValidationServlet extends HttpServlet {

    private  UserService userService;
    private static AuthenticationController authenticationController;
    private String domain;
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

        final String accessToken = req.getParameter("accessToken");
        final String idToken = req.getParameter( "idToken");

        try {
            res.setContentType("application/json");
            // Get or create the current session
            HttpSession session = req.getSession(true);
            // Get and Set the User
            User user  = getUserData(idToken);
            // Initialize User Service and store it in the session
            userService = new UserService(user);
            session.setAttribute("userService", userService);

            Gson gson = new Gson();
            String payload = gson.toJson(user);
            res.getWriter().write(payload);

            res.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e){
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Unauthorized access");
        }
    }

    private User getUserData(String idToken) {
        try {
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
        } catch (Exception e) {
            throw new InvalidParameterException("JWT validation failed: " + e.getMessage());
        }
    }

    private DecodedJWT validateToken(String token) {
        // This can involve checking the signature, expiry, etc.
        try {
            final DecodedJWT jwt = JWT.decode(token);
            JwkProvider jwkProvider =  AuthenticationControllerProvider.getJwkProvider();
            if (!jwt.getIssuer().contains(domain)) {
                throw new InvalidParameterException(String.format("Unknown Issuer %s", jwt.getIssuer()));
            }
            RSAPublicKey publicKey = loadPublicKey(jwt);

            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(jwt.getIssuer())
                    .build();

            verifier.verify(token);
            return jwt;

        } catch (Exception e) {
            //Invalid token
            e.printStackTrace();
            throw new InvalidParameterException("JWT validation failed: "+e.getMessage());
        }
    }

    private RSAPublicKey loadPublicKey(DecodedJWT token) throws JwkException, MalformedURLException {
        JwkProvider provider = AuthenticationControllerProvider.getJwkProvider(); //UrlJwkProvider(("https://" + domain + "/"));

        return (RSAPublicKey) provider.get(token.getKeyId()).getPublicKey();
    }
}


