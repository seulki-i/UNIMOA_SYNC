package net.infobank.common;

import net.infobank.common.service.TsRsDelayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @author skkim
 * @since 2023-02-01
 */
@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);

    private final TsRsDelayService tsRsDelayService;

    public CommandLineAppStartupRunner(TsRsDelayService tsRsDelayService) {
        this.tsRsDelayService = tsRsDelayService;
    }

    @Override
    public void run(String... args) {
        logger.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        try {
            tsRsDelayService.insert();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
