package net.infobank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author skkim
 * @since 2023-02-17
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
}
