package ch.fhnw.modeller.webservice.filter;

import ch.fhnw.modeller.auth.AuthenticationControllerProvider;
import ch.fhnw.modeller.auth.SessionValidationServlet;
import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.model.auth.User;
import ch.fhnw.modeller.webservice.ontology.OntologyManager;
import com.auth0.AuthenticationController;
import com.google.gson.Gson;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

@Provider
public class CookieRequestFilter implements ContainerRequestFilter {
    Gson gson = new Gson();

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Map<String, Cookie> cookies = requestContext.getCookies();
        Cookie idTokenCookie = cookies.get("idToken");
        Cookie accessTokenCookie = cookies.get("accessToken");


        HttpSession session = request.getSession();

        if (idTokenCookie != null && accessTokenCookie != null && SessionValidationServlet.domain!=null) {
            //Check domain and tokens
            String idToken = idTokenCookie.getValue();
            String accessToken = accessTokenCookie.getValue();

            if (requestContext.getProperty("userService") == null ) {
                // Only create userService for a new session
                User user = SessionValidationServlet.getUserData(idToken);
                UserService userService = new UserService(user);
                OntologyManager ontology = OntologyManager.getInstance();
                requestContext.setProperty("userService", userService);
                ontology.setUserService(requestContext);
            } else { // If the userService already exists in session, Retrieve it.
                OntologyManager ontology =  OntologyManager.getInstance();
            }
        }
    }
}
