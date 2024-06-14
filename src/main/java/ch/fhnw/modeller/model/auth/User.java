package ch.fhnw.modeller.model.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class User {
    private String sub;
    private String aud;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    @JsonProperty("updated_at")
    private String updatedAt;
    private String iss;
    private String nickname;
    private String name;

    @JsonProperty("exp")
    private String exp;

    @JsonProperty("iat")
    private String iat;

    private String picture;
    private String email;
    private String sid;

}
