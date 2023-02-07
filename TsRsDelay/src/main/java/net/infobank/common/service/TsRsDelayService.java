package net.infobank.common.service;

import net.infobank.common.dto.AlertInfoDTO;
import net.infobank.common.dto.MessageDTO;
import net.infobank.common.dto.RsCountDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-02-06
 */
@Service
public class TsRsDelayService {
    private static final Logger logger = LoggerFactory.getLogger(TsRsDelayService.class);

    private final JdbcTemplate newAuthDbJdbcTemplate;

    private final JdbcTemplate alertEmmaJdbcTemplate;
    private final JdbcTemplate uniRs1JdbcTemplate;
    private final JdbcTemplate uniRs2JdbcTemplate;
    private final JdbcTemplate uniRs3JdbcTemplate;
    private final JdbcTemplate uniRs4JdbcTemplate;
    private final JdbcTemplate grs1JdbcTemplate;
    private final JdbcTemplate grs2JdbcTemplate;
    private final JdbcTemplate rcs1JdbcTemplate;
    private final JdbcTemplate rcs2JdbcTemplate;

    public TsRsDelayService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate,
                            @Qualifier("alertEmmaJdbcTemplate") JdbcTemplate alertEmmaJdbcTemplate,
                            @Qualifier("uniRs1JdbcTemplate") JdbcTemplate uniRs1JdbcTemplate,
                            @Qualifier("uniRs2JdbcTemplate") JdbcTemplate uniRs2JdbcTemplate,
                            @Qualifier("uniRs3JdbcTemplate") JdbcTemplate uniRs3JdbcTemplate,
                            @Qualifier("uniRs4JdbcTemplate") JdbcTemplate uniRs4JdbcTemplate,
                            @Qualifier("grs1JdbcTemplate") JdbcTemplate grs1JdbcTemplate,
                            @Qualifier("grs2JdbcTemplate") JdbcTemplate grs2JdbcTemplate,
                            @Qualifier("rcs1JdbcTemplate") JdbcTemplate rcs1JdbcTemplate,
                            @Qualifier("rcs2JdbcTemplate") JdbcTemplate rcs2JdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
        this.alertEmmaJdbcTemplate = alertEmmaJdbcTemplate;
        this.uniRs1JdbcTemplate = uniRs1JdbcTemplate;
        this.uniRs2JdbcTemplate = uniRs2JdbcTemplate;
        this.uniRs3JdbcTemplate = uniRs3JdbcTemplate;
        this.uniRs4JdbcTemplate = uniRs4JdbcTemplate;
        this.grs1JdbcTemplate = grs1JdbcTemplate;
        this.grs2JdbcTemplate = grs2JdbcTemplate;
        this.rcs1JdbcTemplate = rcs1JdbcTemplate;
        this.rcs2JdbcTemplate = rcs2JdbcTemplate;
    }

    public void insert() {
        String selectQuery =
                "SELECT alertinfo_key, alert_code, allow, alert_id, alert_callback, fault_type, alert_repeat, alert_period, alert_sendcnt, alert_sendtime " +
                        " FROM alertinfo WHERE alert_code = '1002' AND fault_type = '10022' AND allow IN ('Y', 'P') ORDER BY fault_type, alert_id";


        List<AlertInfoDTO> list = new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new AlertInfoDTO(
                rs.getInt("alertinfo_key"),
                rs.getInt("alert_code"),
                rs.getString("allow"),
                rs.getString("alert_id"),
                rs.getString("alert_callback"),
                rs.getString("fault_type"),
                rs.getInt("alert_repeat"),
                rs.getInt("alert_period"),
                rs.getInt("alert_sendcnt"),
                rs.getTimestamp("alert_sendtime").toLocalDateTime()
        )));

        String tableDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (AlertInfoDTO alert : list) {
            for (RsCountDTO rsData : rsList()) {
                if (rsData.getCount() > 10000) {
                    String alertInsertCheck = "Y";
                    String alertInsertString = "[" + rsData.getRsId() + " " + NumberFormat.getInstance().format(rsData.getCount()) + "]";

                    logger.info("알람발생여부 : " + alertInsertCheck + " : " + alert.getId() + "(" + alert.getKey() + ") : TS>RS Wait : " + alertInsertString);

                    String insertQuery =
                            "INSERT INTO alertlog_" + tableDate + " VALUES (?,?,?,?,?,?,?,?)";

                    KeyHolder keyHolder = new GeneratedKeyHolder();

                    PreparedStatementCreator preparedStatementCreator = (connection) -> {
                        PreparedStatement ps = connection.prepareStatement(insertQuery, new String[]{"alertinfo_key"});
                        ps.setInt(1, alert.getKey());
                        ps.setString(2, "");
                        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setString(4, "N");
                        ps.setString(5, rsData.getRsId());
                        ps.setInt(6, alert.getCode());
                        ps.setString(7, alert.getFaultType());
                        ps.setInt(8, Math.toIntExact(rsData.getCount()));
                        return ps;
                    };

                    newAuthDbJdbcTemplate.update(preparedStatementCreator, keyHolder);

                    long alertInfoKey = keyHolder.getKey().longValue();

                    String alertEmmaKey = alert.getKey() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

                    //문자 메시지 보내는 조건 조회
                    String smsSendCheck = smsSendYn(alert.getRepeat(), alert.getSendCount(), alert.getSendTime(), alert.getPeriod(), alert.getAllow());

                    logger.info("1차 : " + smsSendCheck);

                    if (smsSendCheck.equals("Y")) {
                        String smsSendMessage = smsSendMessage(alert.getCode(), alert.getFaultType(), "", alertInsertString);

                        //수신할 대상 조회
                        String selectQuery2 =
                                "SELECT alert_recipient FROM alert_recipient WHERE alertinfo_key = " + alert.getKey();

                        List<String> userList = new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) ->
                                rs.getString("alert_recipient")));

                        for (String number : userList) {
                            smsSendAction(number, alert.getCallback(), smsSendMessage, alertEmmaKey);
                        }

                        //발송 후 alertinfo update


                    }


                }
            }
        }

    }

    public List<RsCountDTO> rsList() {
        List<RsCountDTO> resultList = new ArrayList<>();

        String selectQuery =
                "SELECT COUNT(tran_pr) AS delaycnt " +
                        " FROM mt_tran " +
                        " WHERE tran_status = '2' AND tran_recvdate >= DATE_ADD(TIMESTAMP(CURRENT_TIME), INTERVAL -1 HOUR) AND TIMESTAMPDIFF(SECOND,tran_recvdate ,tran_rssentdate) > 60";

        resultList.add(uniRs1JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "unirs1",
                rs.getLong("delaycnt")
        )));

        resultList.add(uniRs2JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "unirs2",
                rs.getLong("delaycnt")
        )));

        resultList.add(uniRs3JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "unirs3",
                rs.getLong("delaycnt")
        )));

        resultList.add(uniRs4JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "unirs4",
                rs.getLong("delaycnt")
        )));

        resultList.add(grs1JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "grs1",
                rs.getLong("delaycnt")
        )));

        resultList.add(grs2JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "grs2",
                rs.getLong("delaycnt")
        )));

        resultList.add(rcs1JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "rcs1",
                rs.getLong("delaycnt")
        )));

        resultList.add(rcs2JdbcTemplate.queryForObject(selectQuery, (rs, i) -> new RsCountDTO(
                "rcs2",
                rs.getLong("delaycnt")
        )));

        return resultList;
    }

    public String smsSendYn(int repeat, int sendCount, LocalDateTime sendTime, int period, String allow) {
        String result = "N";

        if (allow.equals("Y")) {
            if (repeat == 0) {
                //repeat가 0이면 제한없이 보냄
                result = "Y";
            } else {
                if (repeat > sendCount) {
                    result = "Y";
                }
            }

            //현재시간 - alert_sendtime의 시간차가 alert_period보다 작으면 보내지 않음
            if (result.equals("Y")) {
                long between = ChronoUnit.MINUTES.between(sendTime, LocalDateTime.now());

                if (between >= period) {
                    result = "Y";
                } else {
                    result = "N";
                }
            }
        }
        return result;
    }

    public String smsSendMessage(int code, String faultType, String message1, String message2) {
        String selectQuery =
                "SELECT A.message AS m1, B.type_message AS m2, B.value_message AS m3 " +
                        "FROM alertcode_msg AS A, faulttype_msg AS B " +
                        "WHERE A.alert_code = " + code + " AND B.fault_type = '" + faultType + "'";

        MessageDTO message = newAuthDbJdbcTemplate.queryForObject(selectQuery, (rs, i) -> new MessageDTO(
                rs.getString("m1"),
                rs.getString("m2"),
                rs.getString("m3")
        ));

        return (message.getM1() + " " + message.getM2()).replace("%s", message1) +
                " " + message.getM3().replace("%d", message2) + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    public String smsSendAction(String recipient, String callback, String smsSendMessage, String alertEmmaKey) {
        String result = "N";

        String insertQuery =
                "INSERT INTO em_smt_tran (" +
                        "alert_emma_key, priority, date_client_req, content, callback, service_type, msg_status, broadcast_yn, recipient_num, country_code " +
                        ") VALUES ( " +
                        "'" + alertEmmaKey + "', 'S', SYSDATE(), '" + smsSendMessage + "', '" + callback + "', '0', '1', 'N', '" + recipient + "', '82')";

        int update = alertEmmaJdbcTemplate.update(insertQuery);

        if (update == 1) {
            result = "Y";
        }
        return result;
    }
}
