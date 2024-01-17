package ch.fhnw.modeller.webservice.filter;

import ch.fhnw.modeller.auth.UserService;
import ch.fhnw.modeller.model.auth.User;
import com.google.gson.Gson;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;

@Provider
public class CookieRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Map<String, Cookie> cookies = requestContext.getCookies();
        Cookie idTokenCookie = cookies.get("idToken");
        Cookie accessTokenCookie = cookies.get("accessToken");
        Cookie userDataCookie = cookies.get("userData");

        if (idTokenCookie != null && accessTokenCookie != null && userDataCookie != null) {

            Gson gson = new Gson();
            String idToken = idTokenCookie.getValue();
            String accessToken = accessTokenCookie.getValue();
            String userData = URLDecoder.decode(userDataCookie.getValue(), "UTF-8");

            User user = gson.fromJson(userData, User.class);
            UserService userService = new UserService(user);
        }
    }
}
