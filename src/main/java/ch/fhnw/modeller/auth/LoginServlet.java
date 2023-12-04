package ch.fhnw.modeller.auth;

import com.auth0.AuthenticationController;
import com.google.gson.Gson;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

@WebServlet(urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private AuthenticationController authenticationController;

    @Override
    public void init(ServletConfig config) throws ServletException {


        super.init(config);
        String domain = config.getServletContext().getInitParameter("com.auth0.domain");
        try {
            authenticationController = AuthenticationControllerProvider.getInstance(config);
        } catch (UnsupportedEncodingException e) {
            throw new ServletException("Couldn't create the AuthenticationController instance. Check the configuration.", e);
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        setAccessControlHeaders(res);


        String redirectUri = req.getScheme() + "://" + req.getServerName();
        if ((req.getScheme().equals("http") && req.getServerPort() != 80) || (req.getScheme().equals("https") && req.getServerPort() != 443)) {
            redirectUri += ":" + req.getServerPort();
        }
        redirectUri += "/callback";

        String authorizeUrl = authenticationController
                .buildAuthorizeUrl(req, res, redirectUri)
                .build();

        Gson gson = new Gson();

        String json = gson.toJson(authorizeUrl);
        // Return the URL in the response
        res.setContentType("application/json");
        res.getWriter().write(json);
        //res.sendRedirect(authorizeUrl);
    }

    private void setAccessControlHeaders(HttpServletResponse res) {
        // Set CORS headers
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        res.setHeader("Access-Control-Allow-Credentials", "true");
    }

}