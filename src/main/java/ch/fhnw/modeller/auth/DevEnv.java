package ch.fhnw.modeller.auth;

import ch.fhnw.modeller.webservice.config.ConfigReader;
import com.google.gson.Gson;
import lombok.Getter;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DevEnv extends HttpServlet {
    @Context
    private ContainerRequestContext crc;
    @Getter
    private static String idToken;
    @Getter
    private static String accessToken;
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
//        try {
//            //authenticationController = AuthenticationControllerProvider.getInstance(config);
//            //domain = config.getServletContext().getInitParameter("com.auth0.domain");
//        } catch (UnsupportedEncodingException e) {
//            throw new ServletException("Couldn't create the AuthenticationController instance. Check the configuration.", e);
//        }
    }
    /**
     * Sets the test user for development environment by bypassing the authentication process.
     *
     * @param req the HttpServletRequest object
     * @param res the HttpServletResponse object
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the current thread is interrupted
     */
    public static void setTestUser(HttpServletRequest req, HttpServletResponse res) throws IOException, InterruptedException {
        //Check current environment, if development then bypass authentication process
        String userEmail = System.getenv("USER_EMAIL");
        String userPassword = System.getenv("USER_PASSWORD");
        String tokenEndpoint = "https://"+ AuthenticationControllerProvider.domain + "/oauth/token"; // Replace with your Auth0 domain
        String clientId = AuthenticationControllerProvider.clientId; // Replace with your Client ID
        String clientSecret = AuthenticationControllerProvider.clientSecret; // Optional, for confidential clients

        String requestBody = "grant_type=password&username=" + userEmail +
                "&password=" + userPassword +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret + // Include if applicable
                //"&audience="+ YOUR_API_AUDIENCE + // Used for APIs requiring authentication in Auth0
                "&scope=openid profile email"; // Adjust scope as needed

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Example of parsing the response body to extract the access token
        String responseBody = response.body();
        JSONObject jsonObj = new JSONObject(responseBody);
        idToken = jsonObj.getString("id_token");
        accessToken = jsonObj.getString("access_token"); // Use this token for authenticated requests

        //Add cookies with tokens to skip authentication
        CallbackServlet callbackServlet = new CallbackServlet();
        callbackServlet.addTokenCookies(accessToken, idToken, res, req);
    }
}


