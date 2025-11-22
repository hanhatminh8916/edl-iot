package com.hatrustsoft.bfe_foraiot.model;

public enum AlertType {
    FALL,              // Phát hiện ngã
    HELP_REQUEST,      // Yêu cầu trợ giúp (SOS)
    PROXIMITY,         // Gần khu vực nguy hiểm
    LOW_BATTERY,       // Pin yếu
    OUT_OF_ZONE,       // Ra ngoài khu vực
    ABNORMAL           // Bất thường khác
}
