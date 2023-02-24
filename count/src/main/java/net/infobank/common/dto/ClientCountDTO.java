package net.infobank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author skkim
 * @since 2023-02-22
 */
@Getter
@Setter
public class ClientCountDTO {
    private String clientCode;

    private String serverName;

    private int count;

    private int sms;

    private int url;

    private int mms;

    private int smo;

    private int mmo;

    public ClientCountDTO(String clientCode, String serverName, int count) {
        this.clientCode = clientCode;
        this.serverName = serverName;
        this.count = count;
    }

    public ClientCountDTO(String clientCode, int sms, int url, int mms, int smo, int mmo) {
        this.clientCode = clientCode;
        this.sms = sms;
        this.url = url;
        this.mms = mms;
        this.smo = smo;
        this.mmo = mmo;
    }
}
