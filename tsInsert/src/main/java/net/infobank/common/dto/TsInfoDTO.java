package net.infobank.common.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @author skkim
 * @since 2023-01-27
 */
@Getter
@Setter
public class TsInfoDTO {
    private String tsCode;
    private int tsNet;
    private String tsId;

    private String F10001;

    private String F10002;

    private String F10003;

    private String F30001;

    private String F10011;

    private String F10012;

    private String etc;

    public TsInfoDTO(String tsCode, int tsNet, String tsId, String f10001, String f10002, String f10003, String f30001, String etc) {
        this.tsCode = tsCode;
        this.tsNet = tsNet;
        this.tsId = tsId;
        this.F10001 = f10001;
        this.F10002 = f10002;
        this.F10003 = f10003;
        this.F30001 = f30001;
        this.etc = etc;
    }

    public TsInfoDTO(String tsCode, int tsNet, String tsId, String f10011, String f10012, String etc) {
        this.tsCode = tsCode;
        this.tsNet = tsNet;
        this.tsId = tsId;
        this.F10011 = f10011;
        this.F10012 = f10012;
        this.etc = etc;
    }
}
