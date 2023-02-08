package net.infobank.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author skkim
 * @since 2023-02-07
 */
@Getter
@Setter
@AllArgsConstructor
public class MoDTO {
    private String moId;
    private String tsCode;

    private String tsId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUpdateDateTime;
}
