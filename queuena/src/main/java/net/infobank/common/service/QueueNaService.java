package net.infobank.common.service;

import net.infobank.common.dto.AlertInfoDTO;
import net.infobank.common.dto.MessageDTO;
import net.infobank.common.dto.QueSessionDTO;
import net.infobank.common.dto.QueueNaDTO;
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
 * @since 2023-02-14
 */
@Service
public class QueueNaService {
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

    public QueueNaService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate,
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

    /**
     * 각 rs에서 queinfo, sessioninfo 테이블에서 데이터를 가져온다 - updatetime이 현재시간과 차이가 60초보다 클때 n/a 값으로
     * alert_queuena 테이블의 7일이 지난 데이터는 삭제한다
     * 각 rs에서 가져온 데이터를 alert_queuena에 저장한다 - n/a 인 값이 있으면 문자전송
     */
    public void insert() {
        logger.info("START");

        //7일 지난 데이터 삭제
        String deleteQuery =
                "DELETE FROM alert_queuena WHERE que_regdatetime <= DATE_ADD(TIMESTAMP(CURRENT_DATE), INTERVAL -7 DAY) ";

        newAuthDbJdbcTemplate.update(deleteQuery);

        //queue, session info 테이블 값 저장
        String insertQuery =
                "INSERT alert_queuena (que_name, que_cnt, que_updatetime, que_type, que_server, que_regdatetime, cnt_flag ) VALUES ";

        for (QueSessionDTO rsData : rsData()) {
            insertQuery += "('" + rsData.getQueName() + "'," + rsData.getCountItem() + ",'" + rsData.getUpdateDateTime() + "','1002','" + rsData.getRsId() + "', SYSDATE(), 'Y'),";
        }

        insertQuery = StringUtils.chop(insertQuery);

        newAuthDbJdbcTemplate.update(insertQuery);

        //alertinfo 데이터
        String selectQuery =
                "SELECT alertinfo_key, alert_code, allow, alert_id, alert_callback, fault_type, alert_repeat, alert_period, alert_sendcnt, alert_sendtime " +
                        " FROM alertinfo WHERE alert_code = '1002' AND fault_type = '1002' AND allow IN ('Y', 'P') ORDER BY fault_type, alert_id";

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

        if (list.size() > 0) {
            String tableDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));

            for (AlertInfoDTO alert : list) {
                String selectQuery2 =
                        "SELECT que_server, que_name, que_updatetime FROM alert_queuena WHERE que_cnt = 'n/a' AND cnt_flag = 'Y'";

                List<QueueNaDTO> queueNaList = newAuthDbJdbcTemplate.query(selectQuery2, (rs, i) -> new QueueNaDTO(
                        rs.getString("que_server"),
                        rs.getString("que_name"),
                        rs.getTimestamp("que_updatetime").toLocalDateTime()
                ));

                if (queueNaList.size() > 0) {
                    for (QueueNaDTO queueNa : queueNaList) {
                        String alertInsertCheck = "Y";
                        String alertInsertString = queueNa.getServer() + " " + queueNa.getName() + "(" + queueNa.getUpdateDateTime() + ")";

                        logger.info("알람발생여부 : " + alertInsertCheck + " : " + alert.getId() + "(" + alert.getKey() + ") : n/a : " + queueNa.getServer() + " " + queueNa.getName() + "(" + queueNa.getUpdateDateTime() + ")");

                        String insertQuery2 =
                                "INSERT INTO alertlog_" + tableDate + "(alertinfo_key, emma_key, alert_recvtime, alert_send, alert_id, alert_code, fault_type, fault_value, fault_src) " +
                                        " VALUES (?,?,?,?,?,?,?,?,?)";

                        KeyHolder keyHolder = new GeneratedKeyHolder();

                        PreparedStatementCreator preparedStatementCreator = (connection) -> {
                            PreparedStatement ps = connection.prepareStatement(insertQuery2, new String[]{"alert_seq"});
                            ps.setInt(1, alert.getKey());
                            ps.setString(2, "");
                            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                            ps.setString(4, "N");
                            ps.setString(5, queueNa.getServer());
                            ps.setInt(6, alert.getCode());
                            ps.setString(7, alert.getFaultType());
                            ps.setInt(8, 0);
                            ps.setString(9, queueNa.getName());
                            return ps;
                        };

                        newAuthDbJdbcTemplate.update(preparedStatementCreator, keyHolder);

                        long alertLogKey = keyHolder.getKey().longValue();

                        String alertEmmaKey = alertLogKey + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

                        //문자 메시지 보내는 조건 조회
                        String smsSendCheck = smsSendYn(alert.getRepeat(), alert.getSendCount(), alert.getSendTime(), alert.getPeriod(), alert.getAllow());

                        logger.info("1차 : " + smsSendCheck);

                        if (smsSendCheck.equals("Y")) {
                            String sendMessage = sendMessage(alert.getCode(), alert.getFaultType(), "", alertInsertString);

                            //문자 전송
                            for (String number : recipientList(alert.getKey())) {
                                smsSendAction(number, alert.getCallback(), sendMessage, alertEmmaKey);
                            }

                            //발송 후 alertinfo update
                            alertInfoUpdate(alert.getKey(), tableDate, alertEmmaKey, alertLogKey);

                            logger.info("실제 알람여부 : " + smsSendCheck + "(" + sendMessage + ")");
                        }
                    }
                }
            }
        }
        logger.info("END");
    }

    public List<QueSessionDTO> rsData() {
        List<QueSessionDTO> resultList = new ArrayList<>();

        String selectQuery =
                "SELECT 'R' AS gubun, NAME AS quename, IF(TIMESTAMPDIFF(SECOND, updatetime, SYSDATE()) > 60, 'n/a', cnt_item) AS cnt_item, updatetime " +
                        "FROM queinfo " +
                        "WHERE NAME LIKE 'db%' OR NAME LIKE 'recv%' " +
                        "UNION ALL " +
                        "SELECT 'S' AS gubun, sendque_name AS quename, IF(alive = 'N' OR TIMESTAMPDIFF(SECOND, updatetime, SYSDATE()) > 60, 'n/a', cnt_wait) AS cnt_item, updatetime " +
                        "FROM sendinginfo";

        resultList.addAll(smrsJdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "smrs",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(unirs1JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "unirs1",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(unirs2JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "unirs2",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(unirs3JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "unirs3",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(unirs4JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "unirs4",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(grs1JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "wngrs01",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(grs2JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "wngrs02",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(rcs1JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "rcsrs1",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        resultList.addAll(rcs2JdbcTemplate.query(selectQuery, (rs, i) -> new QueSessionDTO(
                "rcsrs2",
                rs.getString("gubun"),
                rs.getString("quename"),
                rs.getString("cnt_item"),
                rs.getTimestamp("updatetime").toLocalDateTime()
        )));

        return resultList;
    }

    public List<String> recipientList(int key) {
        String selectQuery =
                "SELECT alert_recipient FROM alert_recipient WHERE alertinfo_key = " + key;

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("alert_recipient")));
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
