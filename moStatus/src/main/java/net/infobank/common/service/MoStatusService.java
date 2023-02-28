package net.infobank.common.service;

import net.infobank.common.dto.MoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-02-07
 */
@Service
public class MoStatusService {
    private static final Logger logger = LoggerFactory.getLogger(MoStatusService.class);

    private final JdbcTemplate newAuthDbJdbcTemplate;
    private final JdbcTemplate alertEmmaJdbcTemplate;
    private final JdbcTemplate mots1JdbcTemplate;
    private final JdbcTemplate mots2JdbcTemplate;

    public MoStatusService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate,
                           @Qualifier("alertEmmaJdbcTemplate") JdbcTemplate alertEmmaJdbcTemplate,
                           @Qualifier("mots1JdbcTemplate") JdbcTemplate mots1JdbcTemplate,
                           @Qualifier("mots2JdbcTemplate") JdbcTemplate mots2JdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
        this.alertEmmaJdbcTemplate = alertEmmaJdbcTemplate;
        this.mots1JdbcTemplate = mots1JdbcTemplate;
        this.mots2JdbcTemplate = mots2JdbcTemplate;
    }

    /**
     * ts_mo_status 테이블에서 date_lastupdate가 5분 지났을 경우 데이터 수집(5분 지나면 무조건 장애로그 발생함)
     * alert_mostatus 테이블에서 tscode 값으로 뮨자발송 여부를 확인하고 10분이 지낫으면 alert_mostatus 테이블 update, 해당 인원에게 문자 발송
     * 문자발송건이 없다면 alert_mostatus 테이블 insert, 김현빈팀장에게만 문자 발송
     */
    public void sendAlert() {
        logger.info("START");

        for (MoDTO mo : moData()) {
            logger.info(mo.toString());

            String alertEmmaKey = "1_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String sendMessage = mo.getMoId() + " " + mo.getTsCode() + "[" + mo.getTsId() + "] " + mo.getLastUpdateDateTime() + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
            logger.info(sendMessage);

            //tscode 기준으로 알람문자가 나갔는지 확인
            String selectQuery =
                    "SELECT alert_sendtime FROM alert_mostatus WHERE tscode = '" + mo.getTsCode() + "'";
            try {
                //있을때
                LocalDateTime sendTime = newAuthDbJdbcTemplate.queryForObject(selectQuery, (rs, i) ->
                        rs.getTimestamp("alert_sendtime").toLocalDateTime());

                long between = ChronoUnit.MINUTES.between(sendTime, LocalDateTime.now());

                logger.info("alert sendtime >> " + sendTime + " - " + LocalDateTime.now() + " - " + between);

                if (between > 10) {
                    String updateQuery =
                            "UPDATE alert_mostatus SET " +
                                    "date_lastupdate = '" + mo.getLastUpdateDateTime() + "', " +
                                    "alert_sendcnt = alert_sendcnt + 1, " +
                                    "alert_sendtime = NOW() " +
                                    "WHERE tscode = '" + mo.getTsCode() + "' AND tsid = '" + mo.getTsId() + "'";

                    newAuthDbJdbcTemplate.update(updateQuery);
//                    logger.info("있을때 : " + updateQuery);

                    for (String number : recipientList()) {
                        smsSendAction(number, "0316281500", sendMessage, alertEmmaKey);
                    }

                    logger.info("10 Min Term SEND");
                }
            } catch (EmptyResultDataAccessException e) {
                String insertQuery =
                        "INSERT INTO alert_mostatus (" +
                                "serverid, tscode, tsid, date_lastupdate, regdate, alert_sendcnt, alert_sendtime " +
                                ") VALUES ( " +
                                "'" + mo.getMoId() + "', '" + mo.getTsCode() + "', '" + mo.getTsId() + "', '" + mo.getLastUpdateDateTime() + "', SYSDATE(), 1, SYSDATE())";

                newAuthDbJdbcTemplate.update(insertQuery);
//                logger.info("없을때 : " +insertQuery);

                smsSendAction("01072719753", "0316281500", sendMessage, alertEmmaKey); //김현빈

                logger.info("New Insert SEND");
            } catch (Exception e) {
                logger.error(String.valueOf(e));
            }
        }
        logger.info("END");
    }

    public List<MoDTO> moData() {
        List<MoDTO> resultList = new ArrayList<>();
        String selectQuery =
                "SELECT tscode, tsid, date_lastupdate " +
                        "FROM ts_mo_status " +
                        "WHERE date_lastupdate <= DATE_ADD(NOW(), INTERVAL -5 MINUTE) AND tsid NOT IN ('smo_sj1', 'smo_sj5', 'smo_sj4', 'smo_sj6', 'mmo_sj1', 'mmo_sj5', 'mmo_sj4', 'mmo_sj6') " +
                        "GROUP BY tscode";

        resultList.addAll(mots1JdbcTemplate.query(selectQuery, (rs, i) -> new MoDTO(
                "newmots1",
                rs.getString("tscode"),
                rs.getString("tsid"),
                rs.getTimestamp("date_lastupdate").toLocalDateTime()
        )));

        resultList.addAll(mots2JdbcTemplate.query(selectQuery, (rs, i) -> new MoDTO(
                "newmots2",
                rs.getString("tscode"),
                rs.getString("tsid"),
                rs.getTimestamp("date_lastupdate").toLocalDateTime()
        )));
        return resultList;
    }

    public List<String> recipientList() {
        String selectQuery =
                "SELECT alert_recipient FROM alert_recipient_etc WHERE etc_key = 'moStatus'";

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("alert_recipient")));
    }

    public String smsSendAction(String recipient, String callback, String sendMessage, String alertEmmaKey) {
        String result = "N";

        String insertQuery =
                "INSERT INTO em_smt_tran (" +
                        "alert_emma_key, priority, date_client_req, content, callback, service_type, msg_status, broadcast_yn, recipient_num, country_code " +
                        ") VALUES ( " +
                        "'" + alertEmmaKey + "', 'S', SYSDATE(), '" + sendMessage + "', '" + callback + "', '0', '1', 'N', '" + recipient + "', '82')";

//        logger.info("문자전송");
//        logger.info(insertQuery);

        int update = alertEmmaJdbcTemplate.update(insertQuery);

        if (update == 1) {
            result = "Y";
        }
        return result;

//        return "Y";
    }
}
