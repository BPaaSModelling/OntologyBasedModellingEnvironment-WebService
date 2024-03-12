package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.config.ConfigReader;
import com.auth0.AuthenticationController;
import com.auth0.IdentityVerificationException;
import com.auth0.SessionUtils;
import com.auth0.Tokens;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.security.interfaces.RSAPublicKey;

/**
 * The Servlet endpoint used as the callback handler in the OAuth 2.0 authorization code grant flow.
 * It will be called with the authorization code after a successful login.
 */
@WebServlet(urlPatterns = {"/callback"})
public class CallbackServlet extends HttpServlet {

    private String redirectOnSuccess;
    private String redirectOnFail;
    private AuthenticationController authenticationController;
    private  UserService userService;
    private String domain;
    private Gson gson = new Gson();


    /**
     * Initialize this servlet with required configuration.
     * <p>
     * Parameters needed on the Local Servlet scope:
     * <ul>
     * <li>'com.auth0.redirect_on_success': where to redirect after a successful authentication.</li>
     * <li>'com.auth0.redirect_on_error': where to redirect after a failed authentication.</li>
     * </ul>
     * Parameters needed on the Local/Global Servlet scope:
     * <ul>
     * <li>'com.auth0.domain': the Auth0 domain.</li>
     * <li>'com.auth0.client_id': the Auth0 Client id.</li>
     * <li>'com.auth0.client_secret': the Auth0 Client secret.</li>
     * </ul>
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        redirectOnSuccess = "/portal/home";
        redirectOnFail = "/login";

        try {
            authenticationController = AuthenticationControllerProvider.getInstance(config);
            domain = config.getServletContext().getInitParameter("com.auth0.domain");
        } catch (UnsupportedEncodingException e) {
            throw new ServletException("Couldn't create the AuthenticationController instance. Check the configuration.", e);
        }
    }

    /**
     * Process a call to the redirect_uri with a GET HTTP method.
     *
     * @param req the received request with the tokens (implicit grant) or the authorization code (code grant) in the parameters.
     * @param res the response to send back to the server.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }


    /**
     * Process a call to the redirect_uri with a POST HTTP method. This occurs if the authorize_url included the 'response_mode=form_post' value.
     * This is disabled by default. On the Local Servlet scope you can specify the 'com.auth0.allow_post' parameter to enable this behaviour.
     *
     * @param req the received request with the tokens (implicit grant) or the authorization code (code grant) in the parameters.
     * @param res the response to send back to the server.
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    private void handle(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        try {
            Tokens tokens = authenticationController.handle(req, res);
            //SessionUtils.set(req, "accessToken", tokens.getAccessToken());
            //SessionUtils.set(req, "idToken", tokens.getIdToken());
            //res.sendRedirect(redirectOnSuccess);

            Gson gson = new Gson();

            String redirectUrl = "";
            String origin = res.getHeader("Origin");
            // if the Config Var is detected then we are on Heroku
            if(System.getenv("WEBAPP")!=null) {
                redirectUrl = System.getenv("WEBAPP").toString() + "/home";
                //redirectUrl = "https://aoame.ch/home";
            } else {
                // Fallback to local URL
                redirectUrl = "http://localhost:4200/home";
            }
            System.out.println("Redirect URL: "+redirectUrl);

            System.out.println("CallbackServlet Tokens are: ID TOKEN: " + tokens.getIdToken());
            System.out.println("ACCESS TOKEN: "+ tokens.getAccessToken());
            addTokenCookies(tokens.getAccessToken(), tokens.getIdToken(), res, req);

            System.out.println("Response headers: " + res.getHeaders("Set-Cookie"));

            String payload = gson.toJson(tokens.getIdToken());
            res.setHeader("Authorization", tokens.getAccessToken());
            res.getWriter().write(payload);

            res.sendRedirect(redirectUrl);
        } catch (IdentityVerificationException e) {
            e.printStackTrace();
            res.sendRedirect(redirectOnFail);
        }


    }
    public void addTokenCookies(String accessToken, String idToken, HttpServletResponse res, HttpServletRequest req) throws UnsupportedEncodingException {
        // Check if the request is secure. You might also need to check the X-Forwarded-Proto header
        // for applications behind a reverse proxy or load balancer.

        String scheme = req.getHeader("X-Forwarded-Proto");
        boolean isSecure = "https".equals(scheme);

        //Add cookies to the HTTP response after token generation
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isSecure);
        accessTokenCookie.setPath("/");

        Cookie idTokenCookie = new Cookie("idToken", idToken);
        idTokenCookie.setHttpOnly(true);
        idTokenCookie.setSecure(isSecure);
        idTokenCookie.setPath("/");
        res.addCookie(accessTokenCookie);
        res.addCookie(idTokenCookie);

        res.addHeader("Set-Cookie", "accessToken="+accessToken+"; HttpOnly; SameSite=None; Secure; Path=/;");
        res.addHeader("Set-Cookie", "idToken="+idToken+"; HttpOnly; SameSite=None; Secure; Path=/;");
//        // Set the domain for the cookies if the app is deployed on Heroku, otherwise the cookies won't be set
//        if(System.getenv("TRIPLESTORE_ENDPOINT")!=null) {
//            accessTokenCookie.setDomain("aoame.herokuapp.com");
//            idTokenCookie.setDomain("aoame.herokuapp.com");
//        }
//        boolean isSecure = req.isSecure() || "https".equals(req.getHeader("X-Forwarded-Proto"));
//
//// Construct cookie strings with SameSite attribute
//        String accessTokenCookie = String.format("accessToken=%s; HttpOnly; Path=/; SameSite=None ", accessToken);
//        String idTokenCookie = String.format("idToken=%s; HttpOnly; Path=/; SameSite=None ", idToken);
//// Optionally add Secure attribute if the request is over HTTPS
//        if (isSecure) {
//            accessTokenCookie += "; Secure";
//            idTokenCookie += "; Secure";
//        }
//// Add the cookies to the response
//        res.addHeader("Set-Cookie", accessTokenCookie);
//        res.addHeader("Set-Cookie", idTokenCookie);
        //res.addHeader("Set-Cookie", "accessToken="+accessToken+"; HttpOnly; SameSite=None; Secure; Path=/;");
        //res.addHeader("Set-Cookie", "idToken="+idToken+"; HttpOnly; SameSite=None; Secure; Path=/;");

    }

}
