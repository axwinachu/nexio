package com.nexio.nexio.email.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectResponse {
    private String authUrl;
}
