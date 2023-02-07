package net.infobank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author skkim
 * @since 2023-02-06
 */
@Getter
@Setter
@AllArgsConstructor
public class RsCountDTO {
    private String rsId;

    private Long count;
}
