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
public class ClientSessionDTO {
    private String clientCode;

    private int mtSessionCount;

    private int smoSessionCount;

    private int mmoSessionCount;

    private int reportSessionCount;
}
