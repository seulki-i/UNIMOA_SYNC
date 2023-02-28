package net.infobank.common.service;

import net.infobank.common.dto.AlertInfoDTO;
import net.infobank.common.dto.ClientCountDTO;
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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-02-22
 */
@Service
public class CountService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JdbcTemplate newAuthDbJdbcTemplate;
    private final JdbcTemplate alertemmaJdbcTemplate;
    private final JdbcTemplate smrsJdbcTemplate;
    private final JdbcTemplate unirs1JdbcTemplate;
    private final JdbcTemplate unirs2JdbcTemplate;
    private final JdbcTemplate unirs3JdbcTemplate;
    private final JdbcTemplate unirs4JdbcTemplate;
    private final JdbcTemplate grs1JdbcTemplate;
    private final JdbcTemplate grs2JdbcTemplate;
    private final JdbcTemplate rcs1JdbcTemplate;
    private final JdbcTemplate rcs2JdbcTemplate;

    public CountService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate,
                        @Qualifier("alertemmaJdbcTemplate") JdbcTemplate alertemmaJdbcTemplate,
                        @Qualifier("smrsJdbcTemplate") JdbcTemplate smrsJdbcTemplate,
                        @Qualifier("unirs1JdbcTemplate") JdbcTemplate unirs1JdbcTemplate,
                        @Qualifier("unirs2JdbcTemplate") JdbcTemplate unirs2JdbcTemplate,
                        @Qualifier("unirs3JdbcTemplate") JdbcTemplate unirs3JdbcTemplate,
                        @Qualifier("unirs4JdbcTemplate") JdbcTemplate unirs4JdbcTemplate,
                        @Qualifier("grs1JdbcTemplate") JdbcTemplate grs1JdbcTemplate,
                        @Qualifier("grs2JdbcTemplate") JdbcTemplate grs2JdbcTemplate,
                        @Qualifier("rcs1JdbcTemplate") JdbcTemplate rcs1JdbcTemplate,
                        @Qualifier("rcs2JdbcTemplate") JdbcTemplate rcs2JdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
        this.alertemmaJdbcTemplate = alertemmaJdbcTemplate;
        this.smrsJdbcTemplate = smrsJdbcTemplate;
        this.unirs1JdbcTemplate = unirs1JdbcTemplate;
        this.unirs2JdbcTemplate = unirs2JdbcTemplate;
        this.unirs3JdbcTemplate = unirs3JdbcTemplate;
        this.unirs4JdbcTemplate = unirs4JdbcTemplate;
        this.grs1JdbcTemplate = grs1JdbcTemplate;
        this.grs2JdbcTemplate = grs2JdbcTemplate;
        this.rcs1JdbcTemplate = rcs1JdbcTemplate;
        this.rcs2JdbcTemplate = rcs2JdbcTemplate;
    }

    public void insert() {
        String selectQuery =
                "SELECT COUNT(*) AS cnt FROM alert_clntcount WHERE cnt_flag = 'P'";

        Long count = newAuthDbJdbcTemplate.queryForObject(selectQuery, Long.TYPE);

        if (count == 0) {
            String updateQuery =
                    "UPDATE alert_clntcount SET cnt_flag = 'P'";

            newAuthDbJdbcTemplate.update(updateQuery);

            LocalDateTime deleteTime = LocalDateTime.now().minusMinutes(61);

            for (ClientCountDTO data : clientCountList()) {
                String insertQuery =
                        "INSERT INTO alert_clntcount_log " +
                                "SELECT * FROM alert_clntcount WHERE client_code = '" + data.getClientCode() + "' AND servername = '" + data.getServerName() + "' " +
                                "AND LEFT(sessiontime, 16) < '" + deleteTime + "'";

                newAuthDbJdbcTemplate.update(insertQuery);

                String deleteQuery =
                        "DELETE FROM alert_clntcount WHERE client_code = '" + data.getClientCode() + "' AND servername = '" + data.getServerName() + "' " +
                                "AND LEFT(sessiontime, 16) < '" + deleteTime + "'";

                newAuthDbJdbcTemplate.update(deleteQuery);
            }

            String[] rsArray = {"smrs", "unirs1", "unirs2", "unirs3", "unirs4", "wngrs01", "wngrs02", "rcsrs1", "rcsrs2"};

            for (String alertId : alertIdList()) {
                for (String rsId : rsArray) {
                    if (sessionInfoList(rsId, alertId).size() > 0) {
                        for (SessionInfoDTO session : sessionInfoList(rsId, alertId)) {

                            String insertQuery = "INSERT INTO alert_clntcount (client_code, connect_date, smsmt1, smsurl1, mmsmt1, smsmo1, mmsmo1, sessiontime, servername, regdate, cnt_flag) " +
                                    "VALUES ('','" + session.getClientCode() + "', '" + session.getConnectDate() + "', ";

                            if (session.getSessionType().equals("MTS")) {
                                insertQuery += session.getSmsMt1Count() + ", " + session.getSmsUrl1Count() + ", " + session.getMmsMt1Count() + ", 0, 0, ";
                            } else if (session.getSessionType().equals("SMOR")) {
                                insertQuery += "0, 0, 0," + session.getSmsMo1Count() + ", 0, ";
                            } else if (session.getSessionType().equals("MMOR")) {
                                insertQuery += "0, 0, 0, 0," + session.getMmsMoo1Count() + ", ";
                            }

                            insertQuery += "'" + session.getSessionTime() + "', '" + session.getRsId() + "', SYSDATE(), 'Y')";

                            newAuthDbJdbcTemplate.update(insertQuery);
                        }
                    } else {
                        // 해당 RS에 정보가 없는 경우 0으로 입력 sessiontime는 시간체크를 위해 now()로 저장
                        String insertQuery =
                                "INSERT INTO alert_clntcount (client_code, connect_date, smsmt1, smsurl1, mmsmt1, smsmo1, mmsmo1, sessiontime, servername, regdate, cnt_flag) " +
                                        "VALUES ('" + alertId + "', '0000-00-00 00:00:00', 0, 0, 0, 0, 0, now(), '" + rsId + "', NOW(), 'Y')";

                        newAuthDbJdbcTemplate.update(insertQuery);
                    }
                }
            }

            String tableDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

            for(AlertInfoDTO alert : alertInfoEmmaList()) {
                LocalDateTime startDateTime = LocalDateTime.now();
                LocalDateTime endDateTime = LocalDateTime.now().minusDays(alert.getPeriod());

                //connectDate 시간체크 - 체크 하려는 시간보다 connectDate 가 작아야함
                if(alert.getConnectDate().isBefore(endDateTime)) {

                    // 해당 아이디 발송건수
                    ClientCountDTO clientCount = clientCountSum(alert.getId(), startDateTime, endDateTime);

                    //알림발송 요건 확인
                    String alertInsertCheck = "N";

                    StringBuilder tempString = new StringBuilder();
                    if(alert.getSms().equals("Y") && clientCount.getSms() < alert.getEmmaCount()) {
                        alertInsertCheck = "Y";
                        tempString.append("sms : 최근 ").append(alert.getEmmaPeriod()).append("분 ").append(NumberFormat.getInstance().format(clientCount.getSms())).append("건,");
                    }
                    if(alert.getUrl().equals("Y") && clientCount.getUrl() < alert.getEmmaCount()) {
                        alertInsertCheck = "Y";
                        tempString.append("url : 최근 ").append(alert.getEmmaPeriod()).append("분 ").append(NumberFormat.getInstance().format(clientCount.getUrl())).append("건,");
                    }
                    if(alert.getMms().equals("Y") && clientCount.getMms() < alert.getEmmaCount()) {
                        alertInsertCheck = "Y";
                        tempString.append("mms : 최근 ").append(alert.getEmmaPeriod()).append("분 ").append(NumberFormat.getInstance().format(clientCount.getMms())).append("건,");
                    }
                    if(alert.getSmo().equals("Y") && clientCount.getSmo() < alert.getEmmaCount()) {
                        alertInsertCheck = "Y";
                        tempString.append("smo : 최근 ").append(alert.getEmmaPeriod()).append("분 ").append(NumberFormat.getInstance().format(clientCount.getSmo())).append("건,");
                    }
                    if(alert.getMmo().equals("Y") && clientCount.getMmo() < alert.getEmmaCount()) {
                        alertInsertCheck = "Y";
                        tempString.append("mmo : 최근 ").append(alert.getEmmaPeriod()).append("분 ").append(NumberFormat.getInstance().format(clientCount.getMmo())).append("건,");
                    }

                    String alertInsertString = StringUtils.chop(String.valueOf(tempString));

                    logger.info("알람 발생 여부 : " + alertInsertCheck + " : " + alert.getId() + "(" + alert.getKey() + ") : " + alert.getEmmaPeriod() + "분 TOTAL > " +
                            clientCount.getSms() + ", " + clientCount.getMms() + ", " + clientCount.getSmo() + ", " + clientCount.getMmo() + " : " + startDateTime + " - " + endDateTime);


                    if(alertInsertCheck.equals("Y")) {
                        String insertQuery =
                                "INSERT INTO alertlog_" + tableDate + "(alertinfo_key, emma_key, alert_recvtime, alert_send, alert_id, alert_code, fault_type, fault_value, fault_src) " +
                                "VALUES (?,?,?,?,?,?,?,?,?)";

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

                        //메시지 전송 조건 조회
                        String smsSendCheck = smsSendYn(alert.getRepeat(), alert.getSendCount(), alert.getSendTime(), alert.getPeriod(), alert.getAllow());

                        logger.info("1차 smsSendCheck : " + smsSendCheck);

                        if(smsSendCheck.equals("Y") && alert.getDayCheck().equals("Y")) {
                            smsSendCheck = smsSendYnCount(alert, LocalDate.now().getDayOfWeek().getValue(), LocalTime.now());

                            logger.info("2차 smsSendCheck : " + smsSendCheck);
                        }

                        if(smsSendCheck.equals("Y")){
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
                    }
                }
            }
        }
        logger.info("END");
    }

    public List<ClientCountDTO> clientCountList() {
        String selectQuery =
                "SELECT client_code, servername, COUNT(*) AS cnt FROM alert_clntcount GROUP BY client_code, servername";

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new ClientCountDTO(
                rs.getString("client_code"),
                rs.getString("servername"),
                rs.getInt("cnt")
        )));
    }

    public List<String> alertIdList() {
        String selectQuery =
                "SELECT alert_id FROM alertinfo WHERE alert_code = '1001' AND fault_type = '20005' AND allow IN ('Y', 'P') group by alert_id";

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("alert_id")));
    }

    public List<SessionInfoDTO> sessionInfoList(String rsId, String alertId) {
        List<SessionInfoDTO> resultList = new ArrayList<>();
        String selectQuery =
                "SELECT pid, group_code, client_code, session_type, connect_date, cnt_smsmt1, cnt_smsurl1, cnt_mmsmt1, cnt_smsmo1, cnt_mmsmo1, sessiontime " +
                        "FROM sessioninfo WHERE session_type IN ('MTS', 'SMOR', 'MMOR') AND client_code = '" + alertId + "'";

        switch (rsId) {
            case "smrs":
                resultList = smrsJdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "smrs",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "unirs1":
                resultList = unirs1JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "unirs1",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "unirs2":
                resultList = unirs2JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "unirs2",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "unirs3":
                resultList = unirs3JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "unirs3",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "unirs4":
                resultList = unirs4JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "unirs4",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "wngrs01":
                resultList = grs1JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "wngrs01",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "wngrs02":
                resultList = grs2JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "wngrs02",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "rcsrs1":
                resultList = rcs1JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "rcsrs1",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
            case "rcsrs2":
                resultList = rcs2JdbcTemplate.query(selectQuery, (rs, i) -> new SessionInfoDTO(
                        "rcsrs2",
                        rs.getInt("pid"),
                        rs.getString("group_code"),
                        rs.getString("client_code"),
                        rs.getString("session_type"),
                        rs.getTimestamp("connect_date").toLocalDateTime(),
                        rs.getInt("cnt_smsmt1"),
                        rs.getInt("cnt_smsurl1"),
                        rs.getInt("cnt_mmsmt1"),
                        rs.getInt("cnt_smsmo1"),
                        rs.getInt("cnt_mmsmo1"),
                        rs.getTimestamp("sessiontime").toLocalDateTime()
                ));
                break;
        }
        return resultList;
    }

    public List<AlertInfoDTO> alertInfoEmmaList() {
        //TODO alert_sendtime 값 0000-00-00 00:00:00 localDateTime으로 못받음
        String selectQuery =
                "SELECT A.alertinfo_key AS alertinfo_key, alert_code, allow, A.alert_id AS alert_id,alert_callback, fault_type, alert_repeat, alert_period, alert_sendcnt, alert_sendtime, " +
                        "emmaperiod, emmacnt, sms, url, mms, smo, mmo, daycheck, format1, starttime1, endtime1, format2, starttime2, endtime2, format3, starttime3, endtime3, " +
                        "(SELECT client_id FROM clientinfo WHERE client_code = A.alert_id) AS client_id, " +
                        "(SELECT connect_date FROM alert_clntcount WHERE client_code = A.alert_id ORDER BY seq DESC LIMIT 1) connect_date " +
                        "FROM alertinfo A, alert_emma B " +
                        "WHERE A.alert_id = B.alert_id AND A.alertinfo_key = B.alertinfo_key AND A.alert_code = '1001' AND A.fault_type = '20005' AND A.allow IN ('Y', 'P') " +
                        "ORDER BY A.alert_id";

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
                rs.getString("alert_sendtime"),
                rs.getInt("emmaperiod"),
                rs.getInt("emmacnt"),
                rs.getString("sms"),
                rs.getString("url"),
                rs.getString("mms"),
                rs.getString("smo"),
                rs.getString("mmo"),
                rs.getString("daycheck"),
                rs.getString("format1"),
                rs.getString("starttime1"),
                rs.getString("endtime1"),
                rs.getString("format2"),
                rs.getString("starttime2"),
                rs.getString("endtime2"),
                rs.getString("format3"),
                rs.getString("starttime3"),
                rs.getString("endtime3"),
                rs.getString("client_id"),
                rs.getTimestamp("connect_date").toLocalDateTime()
        )));
    }

    public ClientCountDTO clientCountSum(String alertId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        String selectQuery =
                "SELECT client_code, " +
                        "SUM(smsmt1) AS sms, SUM(smsurl1) AS url, SUM(mmsmt1) AS mms, SUM(smsmo1) AS smo, SUM(mmsmo1) AS mmo " +
                        "FROM alert_clntcount " +
                        "WHERE client_code = '" + alertId + "' AND LEFT(sessiontime, 16) <= '" + startDateTime + "' AND LEFT(sessiontime, 16) >= '" + endDateTime + "'";

        return newAuthDbJdbcTemplate.queryForObject(selectQuery, (rs, i) -> new ClientCountDTO(
                rs.getString("client_code"),
                rs.getInt("sms"),
                rs.getInt("url"),
                rs.getInt("mms"),
                rs.getInt("smo"),
                rs.getInt("mmo")
        ));
    }

    public String smsSendYn(int repeat, int sendCount, String sendTime, int period, String allow) {
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

            //TODO alert_sendtime 값 0000-00-00 00:00:00 일때 변환 불가 - 시간간격 계산 불가 - 0000-00-00 00:00:00 일땐 무조건 Y?
            //현재시간 - alert_sendtime의 시간차가 alert_period보다 작으면 보내지 않음
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime sendDateTime = LocalDateTime.parse(sendTime, formatter);

            if (result.equals("Y")) {
                long between = ChronoUnit.MINUTES.between(sendDateTime, LocalDateTime.now());

                if (between >= period) {
                    result = "Y";
                } else {
                    result = "N";
                }
            }
        }
        return result;
    }

    public String smsSendYnCount(AlertInfoDTO alert, int nowDay, LocalTime nowTime) {
        switch (nowDay) {
            case 7: //일요일
                return smsSendYnCountSub(alert.getFormat3(), alert.getStartTime3(), alert.getEndTime3(), nowTime);
            case 6 : //토요일
                return smsSendYnCountSub(alert.getFormat2(), alert.getStartTime2(), alert.getEndTime2(), nowTime);
            default:
                return smsSendYnCountSub(alert.getFormat1(), alert.getStartTime1(), alert.getEndTime1(), nowTime);
        }
    }

    public String smsSendYnCountSub(String format, String startTime, String endTime, LocalTime nowTime) {
        LocalTime startTime1 = LocalTime.of(Integer.parseInt(startTime), 0);

        String result = "N";
        if(format.equals("Y")){
            if(startTime.equals("00") && endTime.equals("00")){
                result = "Y";
            } else {
                endTime = (endTime.equals("00") ? "24" : endTime);
                LocalTime endTime1 = LocalTime.of(Integer.parseInt(endTime), 0);

                int i = nowTime.compareTo(startTime1);
                if((nowTime.isAfter(startTime1) || i == 0) && nowTime.isBefore(endTime1)) {
                    result = "Y";
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
