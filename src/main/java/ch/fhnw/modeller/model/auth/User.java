package ch.fhnw.modeller.model.auth;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class User {
    private String sub;
    private List<String> aud;
    private boolean email_verified;
    private String updated_at;
    private Date iss;
    private String nickname;
    private String name;
    private Date exp;
    private Date iat;
    private String picture;
    private String email;
    private String sid;

}
