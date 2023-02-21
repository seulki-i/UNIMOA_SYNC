package net.infobank.common.service;

import net.infobank.common.dto.AlertInfoDTO;
import net.infobank.common.dto.ClientSessionDTO;
import net.infobank.common.dto.MessageDTO;
import net.infobank.common.dto.SessionInfoDTO;
import org.apache.commons.lang3.StringUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-02-17
 */
@Service
public class SessionService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JdbcTemplate newAuthDbJdbcTemplate;

    private final JdbcTemplate alertemmaJdbcTemplate;
    private final JdbcTemplate unirs1JdbcTemplate;
    private final JdbcTemplate unirs2JdbcTemplate;
    private final JdbcTemplate unirs3JdbcTemplate;
    private final JdbcTemplate unirs4JdbcTemplate;
    private final JdbcTemplate grs1JdbcTemplate;
    private final JdbcTemplate grs2JdbcTemplate;

    public SessionService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate,
                          @Qualifier("alertemmaJdbcTemplate") JdbcTemplate alertemmaJdbcTemplate,
                          @Qualifier("unirs1JdbcTemplate") JdbcTemplate unirs1JdbcTemplate,
                          @Qualifier("unirs2JdbcTemplate") JdbcTemplate unirs2JdbcTemplate,
                          @Qualifier("unirs3JdbcTemplate") JdbcTemplate unirs3JdbcTemplate,
                          @Qualifier("unirs4JdbcTemplate") JdbcTemplate unirs4JdbcTemplate,
                          @Qualifier("grs1JdbcTemplate") JdbcTemplate grs1JdbcTemplate,
                          @Qualifier("grs2JdbcTemplate") JdbcTemplate grs2JdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
        this.alertemmaJdbcTemplate = alertemmaJdbcTemplate;
        this.unirs1JdbcTemplate = unirs1JdbcTemplate;
        this.unirs2JdbcTemplate = unirs2JdbcTemplate;
        this.unirs3JdbcTemplate = unirs3JdbcTemplate;
        this.unirs4JdbcTemplate = unirs4JdbcTemplate;
        this.grs1JdbcTemplate = grs1JdbcTemplate;
        this.grs2JdbcTemplate = grs2JdbcTemplate;
    }

    public void insert() {
        logger.info("START");

        //TODO 이 시간이 의미???
        if (LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")).equals("00:01")) {
            String truncateQuery =
                    "TRUNCATE alert_clntsession";

            newAuthDbJdbcTemplate.update(truncateQuery);

            StringBuilder clientCodeString = new StringBuilder();
            for (String clientCode : clientInfoList()) {
                clientCodeString.append("('").append(clientCode).append("'),");
            }

            String clientInsertString = StringUtils.chop(String.valueOf(clientCodeString));

            String insertQuery =
                    "INSERT INTO alert_clntsession (client_code) VALUES " + clientInsertString;

            newAuthDbJdbcTemplate.update(insertQuery);
        }

        String selectQuery =
                "SELECT COUNT(*) AS cnt FROM alert_clntsession WHERE cnt_flag = 'P'";

        Long count = newAuthDbJdbcTemplate.queryForObject(selectQuery, Long.class);

        if (count == 0) {
            String updateQuery =
                    "UPDATE alert_clntsession SET cnt_mtsession = 0, cnt_smosession = 0, cnt_mmosession = 0, cnt_reportsession = 0, cnt_flag = 'P'";

            newAuthDbJdbcTemplate.update(updateQuery);

            for (SessionInfoDTO rsData : rsSessionList()) {
                String insertQuery2 = "";
                if (rsData.getSessionType().equals("MTS")) {
                    insertQuery2 =
                            "INSERT alert_clntsession VALUES ('" + rsData.getClientCode() + "', 1, 0, 0, 0, 'Y') ON DUPLICATE KEY UPDATE cnt_mtsession = cnt_mtsession + 1";
                } else if (rsData.getSessionType().equals("SMOR")) {
                    insertQuery2 =
                            "INSERT alert_clntsession VALUES ('" + rsData.getClientCode() + "', 0, 1, 0, 0, 'Y') ON DUPLICATE KEY UPDATE cnt_smosession = cnt_smosession + 1";
                } else if (rsData.getSessionType().equals("MMOR")) {
                    insertQuery2 =
                            "INSERT alert_clntsession VALUES ('" + rsData.getClientCode() + "', 0, 0, 1, 0, 'Y') ON DUPLICATE KEY UPDATE cnt_mmosession = cnt_mmosession + 1";
                } else if (rsData.getSessionType().equals("MTR")) {
                    insertQuery2 =
                            "INSERT alert_clntsession VALUES ('" + rsData.getClientCode() + "', 0, 0, 0, 1, 'Y') ON DUPLICATE KEY UPDATE cnt_reportsession = cnt_reportsession + 1";
                }

                if (!insertQuery2.equals("")) {
                    newAuthDbJdbcTemplate.update(insertQuery2);
                }
            }

            logger.info("INSERT alert_clntsession END");


            String tableDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            // 클라이언트 접속장애는 연결되지 않은 클라이언트가 많기 때문에 alertinfo에 등록된 리스트만 대상으로 log에 등록
            for (AlertInfoDTO alert : alertInfoSessionList()) {
                ClientSessionDTO clientSession = clientSessionList(alert.getId());

                //알람 발송 요건 확인
                String alertInsertCheck = "N";
                StringBuilder tempString = new StringBuilder();

                if (alert.getMt().equals("Y") && clientSession.getMtSessionCount() == 0) {
                    alertInsertCheck = "Y";
                    tempString.append("mt,");
                }
                if (alert.getSmo().equals("Y") && clientSession.getSmoSessionCount() == 0) {
                    alertInsertCheck = "Y";
                    tempString.append("smo,");
                }
                if (alert.getMmo().equals("Y") && clientSession.getMmoSessionCount() == 0) {
                    alertInsertCheck = "Y";
                    tempString.append("mmo,");
                }
                if (alert.getReport().equals("Y") && clientSession.getReportSessionCount() == 0) {
                    alertInsertCheck = "Y";
                    tempString.append("report,");
                }

                String alertInsertString = StringUtils.chop(String.valueOf(tempString));

                logger.info("알람발생여부 : " + alertInsertCheck + " : " + alert.getId() + "(" + alert.getKey() + ") : " + alertInsertString +
                        " >> MT : " + clientSession.getMtSessionCount() + ", SMO : " + clientSession.getSmoSessionCount() + ", MMO : " + clientSession.getMmoSessionCount() +
                        ", REPORT : " + clientSession.getReportSessionCount());

                if (alertInsertCheck.equals("Y")) {
                    String insertQuery =
                            "INSERT INTO alertlog_" + tableDate + " (alertinfo_key, emma_key, alert_recvtime, alert_send, alert_id, alert_code, fault_type, fault_value, fault_src) " +
                                    " VALUES (?,?,?,?,?,?,?,?,?)";

                    KeyHolder keyHolder = new GeneratedKeyHolder();

                    PreparedStatementCreator preparedStatementCreator = (connection) -> {
                        PreparedStatement ps = connection.prepareStatement(insertQuery, new String[]{"alert_seq"});
                        ps.setInt(1, alert.getKey());
                        ps.setString(2, "");
                        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        ps.setString(4, "N");
                        ps.setString(5, alert.getId());
                        ps.setInt(6, alert.getCode());
                        ps.setString(7, alert.getFaultType());
                        ps.setInt(8, 0);
                        ps.setString(9, alert.getClientId());
                        return ps;
                    };

                    newAuthDbJdbcTemplate.update(preparedStatementCreator, keyHolder);

                    long alertLogKey = keyHolder.getKey().longValue();

                    String alertEmmaKey = alertLogKey + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

                    //메시지 보내는 조건 조회
                    String smsSendCheck = smsSendYn(alert.getRepeat(), alert.getSendCount(), alert.getSendTime(), alert.getPeriod(), alert.getAllow());

                    logger.info("smsSendCheck : " + smsSendCheck);

                    if (smsSendCheck.equals("Y")) {
                        String message = alert.getClientId() + "(" + alert.getId() + ")";

                        String sendMessage = sendMessage(alert.getCode(), alert.getFaultType(), message, alertInsertString);

                        //문자 전송
                        for (String number : recipientList(alert.getKey())) {
                            smsSendAction(number, alert.getCallback(), sendMessage, alertEmmaKey);
                        }

                        //발송 후 alertinfo update
                        alertInfoUpdate(alert.getKey(), tableDate, alertEmmaKey, alertLogKey);

                        logger.info("실제 알람 여부 : " + smsSendCheck + "(" + sendMessage + ")");
                    }
                } else {
                    logger.info("이전 발송시간 : " + alert.getSendTime());

                    if (!alert.getSendTime().toString().equals("0000-00-00 00:00:00")) {
                        String updateQuery2 =
                                "UPDATE alertinfo SET alert_sendtime = '0000-00-00 00:00:00' WHERE alertinfo_key = '" + alert.getKey() + "' AND " +
                                        "alert_id = '" + alert.getId() + "' AND alert_code = '1001' AND fault_type = '20000'";
                        newAuthDbJdbcTemplate.update(updateQuery2);

                        logger.info("UPDATE alertinfo");
                    }
                }
            }
            //모두 완료후엔 flag Update
            String updateQuery3 = "UPDATE alert_clntsession SET cnt_flag = 'Y'";
            newAuthDbJdbcTemplate.update(updateQuery3);
        } else {
            logger.info("이전 세션 프로세스가 끝나지 않음, 실행 안함");
        }
        logger.info("END");
    }

    public List<String> clientInfoList() {
        String selectQuery =
                "SELECT client_code FROM clientinfo";
        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("client_code")));
    }

    public List<SessionInfoDTO> rsSessionList() {
        List<SessionInfoDTO> resultList = new ArrayList<>();

        String selectQuery =
                "SELECT pid, group_code, client_code, session_type FROM sessioninfo";

        resultList.addAll(unirs1JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "unirs1",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        resultList.addAll(unirs2JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "unirs2",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        resultList.addAll(unirs3JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "unirs3",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        resultList.addAll(unirs4JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "unirs4",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        resultList.addAll(grs1JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "wngrs01",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        resultList.addAll(grs2JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                "wngrs02",
                rs.getInt("pid"),
                rs.getString("group_code"),
                rs.getString("client_code"),
                rs.getString("session_type")
        )));

        return resultList;
    }

    public List<AlertInfoDTO> alertInfoSessionList() {
        String selectQuery =
                "SELECT A.alertinfo_key AS alertinfo_key, alert_code, allow, A.alert_id AS alert_id, alert_callback, fault_type, alert_repeat, alert_period, alert_sendcnt, alert_sendtime, mt, smo, mmo, report," +
                        "(SELECT client_id FROM clientinfo WHERE client_code = A.alert_id) AS clientId " +
                        "FROM alertinfo A, alert_session B " +
                        "WHERE A.alert_id = B.alert_id AND A.alert_code = '1001' AND A.fault_type = '20000' AND allow IN ('Y', 'P') ";

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new AlertInfoDTO(
                rs.getInt("alertinfo_key"),
                rs.getInt("alert_code"),
                rs.getString("allow"),
                rs.getString("alert_id"),
                rs.getString("alert_callback"),
                rs.getString("fault_type"),
                rs.getInt("alert_repeat"),
                rs.getInt("alert_period"),
                rs.getInt("alert_sendcnt"),
                rs.getTimestamp("alert_sendtime").toLocalDateTime(),
                rs.getString("mt"),
                rs.getString("smo"),
                rs.getString("mmo"),
                rs.getString("report"),
                rs.getString("clientId")
        )));
    }

    public ClientSessionDTO clientSessionList(String alertId) {
        String selectQuery =
                "SELECT client_code, cnt_mtsession, cnt_smosession, cnt_mmosession, cnt_reportsession " +
                        "FROM alert_clntsession " +
                        "WHERE client_code ='" + alertId + "'";

        return newAuthDbJdbcTemplate.queryForObject(selectQuery, (rs, i) -> new ClientSessionDTO(
                rs.getString("client_code"),
                rs.getInt("cnt_mtsession"),
                rs.getInt("cnt_smosession"),
                rs.getInt("cnt_mmosession"),
                rs.getInt("cnt_reportsession")
        ));
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

    public String sendMessage(int code, String faultType, String message1, String message2) {
        String selectQuery =
                "SELECT A.message AS m1, B.type_message AS m2, B.value_message AS m3 " +
                        "FROM alertcode_msg AS A, faulttype_msg AS B " +
                        "WHERE A.alert_code = " + code + " AND B.fault_type = '" + faultType + "'";

        MessageDTO message = newAuthDbJdbcTemplate.queryForObject(selectQuery, (rs, i) -> new MessageDTO(
                rs.getString("m1"),
                rs.getString("m2"),
                rs.getString("m3")
        ));

        return message.getM1().replace("%s", message1) + " " + message.getM2() + " " + message.getM3().replace("%d", message2) +
                " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
    }

    public List<String> recipientList(int key) {
        String selectQuery =
                "SELECT alert_recipient FROM alert_recipient WHERE alertinfo_key = " + key;

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("alert_recipient")));
    }

    public String smsSendAction(String recipient, String callback, String smsSendMessage, String alertEmmaKey) {
        String result = "N";

        String insertQuery =
                "INSERT INTO em_smt_tran (" +
                        "alert_emma_key, priority, date_client_req, content, callback, service_type, msg_status, broadcast_yn, recipient_num, country_code " +
                        ") VALUES ( " +
                        "'" + alertEmmaKey + "', 'S', SYSDATE(), '" + smsSendMessage + "', '" + callback + "', '0', '1', 'N', '" + recipient + "', '82')";

        int update = alertemmaJdbcTemplate.update(insertQuery);

        if (update == 1) {
            result = "Y";
        }
        return result;
    }

    public void alertInfoUpdate(int alertInfoKey, String tableDate, String alertEmmaKey, long alertLogKey) {
        String updateQuery =
                "UPDATE alertinfo SET alert_sendcnt = alert_sendcnt +1, alert_sendtime = SYSDATE() " +
                        "WHERE alertinfo_key = " + alertInfoKey;

        int result = newAuthDbJdbcTemplate.update(updateQuery);

        if (result == 1) {

        }

        //TODO result 가 1 일때만 아래 쿼리를 실행하야하는게 아닌지?
        String updateQuery2 =
                "UPDATE alertlog_" + tableDate + " SET " +
                        "alert_send = 'Y', emma_key = '" + alertEmmaKey + "' " +
                        "WHERE alert_seq = " + alertLogKey;

        newAuthDbJdbcTemplate.update(updateQuery2);
    }
}
