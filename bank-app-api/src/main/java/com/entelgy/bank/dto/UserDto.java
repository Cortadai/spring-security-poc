package com.entelgy.bank.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class UserDto {

    private long id;
    private String name;
    private String email;
    private String mobileNumber;
    private String role;

}
