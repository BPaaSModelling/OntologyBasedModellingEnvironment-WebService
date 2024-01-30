package ch.fhnw.modeller.auth;

import com.auth0.SessionUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import java.io.IOException;

/**
 * The HomeServlet class is responsible for handling GET requests to the "/portal/home" URL path.
 * It extends the HttpServlet class and provides an implementation for the doGet() method.
 */
@WebServlet(urlPatterns = {"/portal/home"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
//        final String accessToken = (String) SessionUtils.get(req, "accessToken");
//        final String idToken = (String) SessionUtils.get(req, "idToken");
//        if (accessToken != null) {
//            req.setAttribute("userId", accessToken);
//        } else if (idToken != null) {
//            req.setAttribute("userId", idToken);
//        }
//        // TODO: MAKE SURE THE FOLLOWING LINE WORKS
//        System.out.println("Access Token"+accessToken+"IdToken"+idToken);
//        res.sendRedirect("http://localhost:4200/home");


        //req.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(req, res);
    }

}
