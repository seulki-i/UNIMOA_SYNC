package net.infobank.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author skkim
 * @since 2023-02-22
 */
@Getter
@Setter
@AllArgsConstructor
public class AlertInfoDTO {
    private int key;

    private int code;

    private String allow;

    private String id;

    private String callback;

    private String faultType;

    private int repeat;

    private int period;

    private int sendCount;

//    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
//    private LocalDateTime sendTime;
    private String sendTime;

    private int emmaPeriod;

    private int emmaCount;

    private String sms;

    private String url;

    private String mms;

    private String smo;

    private String mmo;

    private String dayCheck;

    private String format1;

    private String startTime1;

    private String endTime1;

    private String format2;

    private String startTime2;

    private String endTime2;

    private String format3;

    private String startTime3;

    private String endTime3;

    private String clientId;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime connectDate;
    private String connectDate;
}
