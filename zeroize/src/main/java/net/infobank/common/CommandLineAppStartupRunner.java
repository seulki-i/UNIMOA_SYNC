package net.infobank.common;

import net.infobank.common.service.ZeroizeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineAppStartupRunner.class);

    private final ZeroizeService zeroizeService;

    public CommandLineAppStartupRunner(ZeroizeService zeroizeService) {
        this.zeroizeService = zeroizeService;
    }

    @Override
    public void run(String... args) {
        logger.info("Application started with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        try {
            zeroizeService.update();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
