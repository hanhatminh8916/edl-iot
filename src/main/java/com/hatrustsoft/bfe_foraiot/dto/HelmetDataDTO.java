package com.hatrustsoft.bfe_foraiot.dto;

import lombok.Data;

@Data
public class HelmetDataDTO {
    private Long helmetId;
    private String eventType;
    private Double temperature;
    private Double heartRate;
    private Double gpsLat;
    private Double gpsLon;
    private Double accelX;
    private Double accelY;
    private Double accelZ;
    private Double gyroX;
    private Double gyroY;
    private Double gyroZ;
    private Integer batteryLevel;
    private Float uwbDistance;
    private Integer rssi;
    private String gatewayId;
}
