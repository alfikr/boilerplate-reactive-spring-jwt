package com.jasavast.resource.vm;

import com.jasavast.service.dto.UserDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class UserVM extends UserDTO {
    private String password;
}
