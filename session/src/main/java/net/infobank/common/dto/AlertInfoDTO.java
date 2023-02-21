package net.infobank.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @author skkim
 * @since 2023-02-17
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

    private String mt;

    private String smo;

    private String mmo;

    private String report;

    private String clientId;
}
