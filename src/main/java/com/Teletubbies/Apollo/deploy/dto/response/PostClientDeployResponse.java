package com.Teletubbies.Apollo.deploy.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PostClientDeployResponse {
    @JsonProperty("stackName")
    private String stackName;
    @JsonProperty("stackType")
    private String stackType;
    @JsonProperty("endpoint")
    private String endPoint;
}