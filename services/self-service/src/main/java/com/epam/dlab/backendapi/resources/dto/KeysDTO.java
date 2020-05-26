package com.epam.dlab.backendapi.resources.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class KeysDTO {
    private String publicKey;
    private String privateKey;
    private String username;
}
