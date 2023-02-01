package net.infobank.common.service;

import net.infobank.common.dto.ZeroiseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * @author skkim
 * @since 2023-01-27
 */
@Service
public class ZeroizeService {
    private static final Logger logger = LoggerFactory.getLogger(ZeroizeService.class);

    private final JdbcTemplate newAuthDbJdbcTemplate;

    public ZeroizeService(JdbcTemplate newAuthDbJdbcTemplate) {
        this.newAuthDbJdbcTemplate = newAuthDbJdbcTemplate;
    }

    /**
     * newsms@203.248.251.219:/sms/www/MOA/batch/alert_crontab_zeroize.php
     * 알람발송 카운트가 풀일때 설정된 시간이 지나면 다시 알람을 발송할 수 있도록 alert_sendcnt를 0으로 초기화한다
     */
    public void update() {
        logger.info("start");
        LocalDateTime startTime = LocalDateTime.now();

        String selectQuery =
                "SELECT alertinfo_key, alert_reset, alert_sendtime, alert_id " +
                        "FROM alertinfo WHERE alert_repeat <= alert_sendcnt AND alert_repeat != 0";

        List<ZeroiseDTO> list = new ArrayList<>(newAuthDbJdbcTemplate.query(selectQuery, (rs, i) -> new ZeroiseDTO(
                rs.getInt("alertinfo_key"),
                rs.getInt("alert_reset"),
                rs.getTimestamp("alert_sendtime").toLocalDateTime(),
                rs.getString("alert_id")
        )));

        for (ZeroiseDTO dto : list) {
            long between = ChronoUnit.MINUTES.between(dto.getSendTime(), LocalDateTime.now());
            String checkUpdate = "N";

            if (between >= dto.getReset()) { // 리셋시간(분)보다 더 지났다면
                checkUpdate = "Y";
                String updateQuery = "UPDATE alertinfo SET alert_sendcnt = 0 WHERE alertinfo_key = '" + dto.getKey() + "'";

                newAuthDbJdbcTemplate.update(updateQuery);
            }

            logger.info("calcTime : " + between + ", sendTime : " + dto.getSendTime() + ", key : " + dto.getKey()
                    + ", alert_id : " + dto.getId() + ", UPDATE : " + checkUpdate);
        }

        LocalDateTime endTime = LocalDateTime.now();

        long between = ChronoUnit.SECONDS.between(startTime, endTime);
        logger.info(" : " + between);

        logger.info("end");
    }
}
