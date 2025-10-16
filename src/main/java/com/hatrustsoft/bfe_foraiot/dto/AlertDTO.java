package com.hatrustsoft.bfe_foraiot.dto;

import lombok.Data;

@Data
public class AlertDTO {
    private Long helmetId;
    private String alertType;
    private String severity;
    private String message;
    private Double gpsLat;
    private Double gpsLon;
}
