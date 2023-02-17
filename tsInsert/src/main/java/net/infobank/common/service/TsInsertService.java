package net.infobank.common.service;

import net.infobank.common.dto.TsInfoDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-02-01
 */
@Service
public class TsInsertService {
    private static final Logger logger = LoggerFactory.getLogger(TsInsertService.class);

    private final JdbcTemplate newAuthDbJdbcTemplate;

    public TsInsertService(@Qualifier("newAuthDbJdbcTemplate") JdbcTemplate newAuthDbJdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
    }

    /**
     * tsinfo 테이블의 tscode값과 alertinfo 테이블의 fault_type값이 같은 경우가 없을때
     * alertinfo, alert_recipient 테이블 insert, alertinfo 테이블 update
     */
    public void InsertAlertInfoTsCode() {
        logger.info("START : tsCode");
        List<Integer> faultList = new ArrayList<>(); //1003_1
        faultList.add(10001);
        faultList.add(10002);
        faultList.add(10003);
        faultList.add(30001);

        StringBuilder select = new StringBuilder();

        for (int i : faultList) {
            select.append("IFNULL(" +
                    "(SELECT alert_id FROM alertinfo WHERE alert_id = ts_code AND alert_code = '1003' AND fault_type = '" + i + "'), '0') AS F" + i + ", ");
        }

        String selectQuery =
                "SELECT * FROM " +
                        "(select ts_code, ts_net, ts_id, " + select + " '' AS etc FROM tsinfo WHERE LEFT(ts_code, 1) != '6') AS T";

        List<TsInfoDTO> list = new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new TsInfoDTO(
                rs.getString("ts_code"),
                rs.getInt("ts_net"),
                rs.getString("ts_id"),
                rs.getString("F10001"),
                rs.getString("F10002"),
                rs.getString("F10003"),
                rs.getString("F30001"),
                rs.getString("etc")
        )));

        for (TsInfoDTO dto : list) {
            for (int i : faultList) {
                if (i == 10001) {
                    if (dto.getF10001().equals("0")) {
                        insertTsCode(dto, i);
                    }
                } else if (i == 10002) {
                    if (dto.getF10002().equals("0")) {
                        insertTsCode(dto, i);
                    }
                } else if (i == 10003) {
                    if (dto.getF10003().equals("0")) {
                        insertTsCode(dto, i);
                    }
                } else if (i == 30001) {
                    if (dto.getF30001().equals("0")) {
                        insertTsCode(dto, i);
                    }
                }
            }
        }
        logger.info("END : tsCode");
    }

    /**
     * tsinfo 테이블의 ts_id값과 alertinfo 테이블의 fault_type값이 같은 경우가 없을때
     * alertinfo, alert_recipient 테이블 insert, alertinfo 테이블 update
     */
    public void InsertAlertInfoTId() {
        logger.info("START : tsId");
        List<Integer> faultList = new ArrayList<>(); //1003_2
        faultList.add(10011);
        faultList.add(10012);

        StringBuilder select = new StringBuilder();

        for (int i : faultList) {
            select.append("IFNULL(" +
                    "(SELECT alert_id FROM alertinfo WHERE alert_id = REPLACE(ts_id, '_', '') AND alert_code = '1003' AND fault_type = '" + i + "'), '0') AS F" + i + ", ");
        }

        String selectQuery =
                "SELECT * FROM " +
                        "(select ts_code, ts_net, REPLACE(ts_id, '_', '') AS ts_id, " + select + " '' AS etc FROM tsinfo WHERE LEFT(ts_code, 1) != '6' GROUP BY REPLACE(ts_id, '_', '')) AS T";

        List<TsInfoDTO> list = new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new TsInfoDTO(
                rs.getString("ts_code"),
                rs.getInt("ts_net"),
                rs.getString("ts_id"),
                rs.getString("F10011"),
                rs.getString("F10012"),
                rs.getString("etc")
        )));

        for (TsInfoDTO dto : list) {
            for (int i : faultList) {
                if (i == 10011) {
                    if (dto.getF10011().equals("0")) {
                        insertTsId(dto, i);
                    }
                } else if (i == 10012) {
                    if (dto.getF10012().equals("0")) {
                        insertTsId(dto, i);
                    }
                }
            }
        }
        logger.info("END : tsId");
    }

    public void insertTsCode(TsInfoDTO dto, int i) {
        String insertQuery =
                "INSERT INTO alertinfo (alert_code, alert_id, allow, alert_callback, alert_repeat, alert_period, alert_sendcnt, alert_recvtime, alert_sendtime, regtime, regperson, updatetime, updateperson, fault_type" +
                        ")  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        //TODO alert_recvtime, alert_sendcnt 날짜 0000-00-00 00:00:00 으로 넣을 수 없음
        PreparedStatementCreator preparedStatementCreator = (connection) -> {
            PreparedStatement prepareStatement = connection.prepareStatement(insertQuery, new String[]{"alertinfo_key"});
            prepareStatement.setInt(1, 1003);
            prepareStatement.setString(2, dto.getTsCode());
            prepareStatement.setString(3, "N");
            prepareStatement.setString(4, "0316281574");
            prepareStatement.setInt(5, 0);
            prepareStatement.setInt(6, 20);
            prepareStatement.setInt(7, 0);
            prepareStatement.setTimestamp(8, Timestamp.valueOf("1970-01-01 00:00:00"));
            prepareStatement.setTimestamp(9, Timestamp.valueOf("1970-01-01 00:00:00"));
            prepareStatement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            prepareStatement.setString(11, "AUTO");
            prepareStatement.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            prepareStatement.setString(13, "GW_TC");
            prepareStatement.setInt(14, i);
            return prepareStatement;
        };

        newAuthDbJdbcTemplate.update(preparedStatementCreator, keyHolder);

        long alertInfoKey = keyHolder.getKey().longValue();

        for (String number : recipientList()) {
            String insertQuery2 =
                    "INSERT INTO alert_recipient (alertinfo_key, alert_recipient) VALUES " +
                            "('" + alertInfoKey + "', '" + number + "') ";

            newAuthDbJdbcTemplate.update(insertQuery2);
        }

        String updateQuery = "UPDATE alertinfo SET allow = 'Y' WHERE alertinfo_key =" + alertInfoKey;
        newAuthDbJdbcTemplate.update(updateQuery);

        logger.info("TS INFO : " + dto.getTsCode() + "(" + dto.getTsId() + ") / " + i);
    }

    public void insertTsId(TsInfoDTO dto, int i) {
        String insertQuery =
                "INSERT INTO alertinfo (alert_code, alert_id, allow, alert_callback, alert_repeat, alert_period, alert_sendcnt, alert_recvtime, alert_sendtime, regtime, regperson, updatetime, updateperson, fault_type" +
                        ")  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        //TODO alert_recvtime, alert_sendcnt 날짜 0000-00-00 00:00:00 으로 넣을 수 없음
        PreparedStatementCreator preparedStatementCreator = (connection) -> {
            PreparedStatement prepareStatement = connection.prepareStatement(insertQuery, new String[]{"alertinfo_key"});
            prepareStatement.setInt(1, 1003);
            prepareStatement.setString(2, dto.getTsId());
            prepareStatement.setString(3, "N");
            prepareStatement.setString(4, "0316281574");
            prepareStatement.setInt(5, 5);
            prepareStatement.setInt(6, 10);
            prepareStatement.setInt(7, 0);
            prepareStatement.setTimestamp(8, Timestamp.valueOf("1970-01-01 00:00:00"));
            prepareStatement.setTimestamp(9, Timestamp.valueOf("1970-01-01 00:00:00"));
            prepareStatement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            prepareStatement.setString(11, "AUTO");
            prepareStatement.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            prepareStatement.setString(13, "GW_TI");
            prepareStatement.setInt(14, i);
            return prepareStatement;
        };

        newAuthDbJdbcTemplate.update(preparedStatementCreator, keyHolder);

        long alertInfoKey = keyHolder.getKey().longValue();

        for (String number : recipientList()) {
            String insertQuery2 =
                    "INSERT INTO alert_recipient (alertinfo_key, alert_recipient) VALUES " +
                            "('" + alertInfoKey + "', '" + number + "') ";

            newAuthDbJdbcTemplate.update(insertQuery2);
        }

        String updateQuery = "UPDATE alertinfo SET allow = 'Y' WHERE alertinfo_key =" + alertInfoKey;
        newAuthDbJdbcTemplate.update(updateQuery);

        logger.info("TS INFO : " + dto.getTsId() + " / " + i);
    }

    public List<String> recipientList() {
        String selectQuery =
                "SELECT alert_recipient FROM alert_recipient_etc WHERE etc_key = 'tsInsert'";

        return new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> rs.getString("alert_recipient")));
    }
}
