package net.infobank.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author skkim
 * @since 2023-02-09
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

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime sendTime;
    private String sendTime;
}
