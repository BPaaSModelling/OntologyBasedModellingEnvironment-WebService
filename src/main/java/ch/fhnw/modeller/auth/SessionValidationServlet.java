package ch.fhnw.modeller.auth;

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
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    private AuthenticationController authenticationController;
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



        if (isValidToken(accessToken) && idToken != null) {

            //String userData = getUserData(accessToken);
            //String userData = getUserData(idToken);

            res.setContentType("application/json");

            Map<String, Object> userData = getUserData(idToken);

            Gson gson = new Gson();
            String payload = gson.toJson(userData);
            res.getWriter().write(payload);

            res.setStatus(HttpServletResponse.SC_OK);
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.getWriter().write("Unauthorized access");
        }
    }

    private boolean isValidToken(String token) {
        // This can involve checking the signature, expiry, etc.

        // Assuming you have methods to get your Auth0 public key, issuer, and audience
        if (token != null) // Additional validations can be added here (like expiry)
            return true;
        else return false;
    }

    private Map<String, Object> getUserData(String idToken) {
        Map<String, Object> claimsMap = new HashMap<>();
        try {
            JwkProvider provider = new UrlJwkProvider(new URL("https://" + domain + "/"));
            DecodedJWT jwt = JWT.decode(idToken);

            // Get all claims
            Map<String, Claim> claims = jwt.getClaims();

//            Algorithm algorithm = Algorithm.RSA256((RSAKeyProvider) provider);
//            JWTVerifier verifier = JWT.require(algorithm)
//                    .withIssuer("auth0")
//                    .build();
//            DecodedJWT verifiedJwt = verifier.verify(idToken);

            // Convert each claim to a simple object
            for (Map.Entry<String, Claim> entry : claims.entrySet()) {
                Claim claim = entry.getValue();
                claimsMap.put(entry.getKey(), claim.as(Object.class));
            }
            return claimsMap;
        } catch (JWTVerificationException exception) {
            //Invalid token
            exception.printStackTrace();
            return null;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


        // Implement logic to fetch user data based on access token
        // Return data as a JSON string
//        try {
//            URL url = new URL("https://"+ domain +"/userinfo");
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//
//            // Set up the headers
//            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
//            conn.setRequestMethod("GET");
//
//            int responseCode = conn.getResponseCode();
//            if (responseCode == HttpURLConnection.HTTP_OK) { // success
//                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//                String inputLine;
//                StringBuffer response = new StringBuffer();
//
//                while ((inputLine = in.readLine()) != null) {
//                    response.append(inputLine);
//                }
//                in.close();
//                // Print result
//                return response.toString();
//            } else {
//                return "GET request failed. HTTP error code : " + responseCode;
//            }
//        } catch(Exception e) {
//            return "Error: " + e.getMessage();
//        }
    }
}


