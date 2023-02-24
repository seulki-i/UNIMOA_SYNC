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
public class SessionInfoDTO {
    private String rsId;

    private int pid;

    private String groupCode;

    private String clientCode;

    private String sessionType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime connectDate;

    private int smsMt1Count;

    private int smsUrl1Count;

    private int mmsMt1Count;

    private int smsMo1Count;

    private int mmsMoo1Count;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sessionTime;
}
