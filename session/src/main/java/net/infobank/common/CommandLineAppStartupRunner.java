package net.infobank.common;

import net.infobank.common.service.SessionService;
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

    private final SessionService sessionService;

    public CommandLineAppStartupRunner(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void run(String... args) {
        logger.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        try {
            sessionService.insert();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
