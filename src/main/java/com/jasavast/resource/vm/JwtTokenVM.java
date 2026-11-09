package com.jasavast.resource.vm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JwtTokenVM(@JsonProperty("id_token") String idToken) {
}
