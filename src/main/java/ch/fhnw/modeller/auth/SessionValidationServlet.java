package ch.fhnw.modeller.auth;

import com.auth0.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.PublicKey;

@WebServlet("/testing")
public class SessionValidationServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        final String accessToken = (String) SessionUtils.get(req, "accessToken");
        final String idToken = (String) SessionUtils.get(req, "idToken");

        if (isValidToken(accessToken) && idToken != null) {
            req.setAttribute("userId", accessToken);
            req.setAttribute("userId", idToken);

            String userData = getUserData(accessToken);
            res.setContentType("application/json");
            res.getWriter().write(userData);
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

    private String getUserData(String accessToken) {
        // Implement logic to fetch user data based on access token
        // Return data as a JSON string
        return "{\"username\": \"exampleUser\"}"; // Placeholder
    }
}


