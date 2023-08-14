package com.Teletubbies.Apollo.auth.dto;

import com.Teletubbies.Apollo.auth.domain.ApolloUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SaveUserRequest {
    @JsonProperty("id")
    private Long oauthId;

    @JsonProperty("login")
    private String login;

    @JsonProperty("name")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("avatar_url")
    private String profileUrl;

    public ApolloUser changeDTOtoObj(SaveUserRequest saveUserRequest) {
        return new ApolloUser(saveUserRequest);
    }
}