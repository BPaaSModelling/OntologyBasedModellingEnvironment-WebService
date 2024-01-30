package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.model.auth.User;
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

            //Add cookies to the HTTP response after token generation
//            Cookie accessTokenCookie = new Cookie("accessToken", tokens.getAccessToken());
//            accessTokenCookie.setHttpOnly(true);
//            accessTokenCookie.setSecure(true);
//            res.addCookie(accessTokenCookie);
//
//            Cookie idTokenCookie = new Cookie("idToken", tokens.getIdToken());
//            idTokenCookie.setHttpOnly(true);
//            idTokenCookie.setSecure(true);
//            res.addCookie(idTokenCookie);



            //Add tokens
            Gson gson = new Gson();
            String payload = gson.toJson(tokens.getIdToken());
            res.setHeader("Authorization", tokens.getAccessToken());
            res.getWriter().write(payload);

            String redirectUrl = "http://localhost:4200/home";
            //redirectUrl += "?accessToken=" + URLEncoder.encode(tokens.getAccessToken(), "UTF-8");
            //redirectUrl += "&idToken=" + URLEncoder.encode(tokens.getIdToken(), "UTF-8");

//            SessionValidationServlet sessionValidationServlet = new SessionValidationServlet();
//            sessionValidationServlet.init(getServletConfig());
//            sessionValidationServlet.doGet(req, res);
            //User user = decodeUserData(tokens.getIdToken());
            //UserService userService = initializeUserService(user);
            addTokenCookies(tokens.getAccessToken(), tokens.getIdToken(), res);

            res.sendRedirect(redirectUrl);
        } catch (IdentityVerificationException e) {
            e.printStackTrace();
            res.sendRedirect(redirectOnFail);
        }


    }
    private void addTokenCookies(String accessToken, String idToken, HttpServletResponse res) throws UnsupportedEncodingException {
        //Add cookies to the HTTP response after token generation
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        accessTokenCookie.setPath("/");
        res.addCookie(accessTokenCookie);

        Cookie idTokenCookie = new Cookie("idToken", idToken);
        idTokenCookie.setHttpOnly(true);
        idTokenCookie.setSecure(false);
        idTokenCookie.setPath("/");
        res.addCookie(idTokenCookie);

//        String encodedUser = URLEncoder.encode(gson.toJson(user), "UTF-8");
//        Cookie userDataCookie = new Cookie("userData", encodedUser);
//        userDataCookie.setHttpOnly(true);
//        userDataCookie.setSecure(true);
//        res.addCookie(userDataCookie);
    }

}
