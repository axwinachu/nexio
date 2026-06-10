package com.nexio.nexio.email.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailStatusResponse {
    private boolean connected;
    private String email;
}
