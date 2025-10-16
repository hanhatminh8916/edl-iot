package com.hatrustsoft.bfe_foraiot.dto;

import lombok.Data;

@Data
public class CommandDTO {
    private String command;
    private String parameter;
    private Long helmetId;
}
