package com.camptocamp.opendata.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

@Value
@With
@Builder
@Accessors(chain = true)
public class AuthCredentials {

    public enum AuthType {
        basic, bearer
    }

    private AuthType type;
    private String userName;
    private String password;
    private String token;
}
