package net.infobank.common;

import net.infobank.common.service.MoStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);

    private final MoStatusService moStatusService;

    public CommandLineAppStartupRunner(MoStatusService moStatusService) {
        this.moStatusService = moStatusService;
    }


    @Override
    public void run(String... args) {
        logger.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        try {
            moStatusService.sendAlert();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
