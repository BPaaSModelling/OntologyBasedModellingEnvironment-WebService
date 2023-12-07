package ch.fhnw.modeller.auth;

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

/**
 * Filter class to check if a valid session exists. This will be true if the User Id is present.
 */
@WebFilter(urlPatterns = "/auth")
public class Auth0Filter implements Filter{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * Perform filter check on this request - verify the User Id is present.
     *
     * @param request  the received request
     * @param response the response to send
     * @param next     the next filter chain
     **/
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain next) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String accessToken = request.getParameter("accessToken");
        String idToken = request.getParameter( "idToken");

        if (accessToken == null && idToken == null) {
            //Do some token validation
            //res.sendRedirect("/login");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        next.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
