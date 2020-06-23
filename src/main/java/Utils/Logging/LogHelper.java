package Utils.Logging;

import Utils.Loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LogHelper {
    public static void initLog(Logger logger, String className, String logname){
        FileHandler fh;
        try {
            new File(ConfigurationLoader.getLogPath()).mkdirs();
            System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
            fh = new FileHandler(ConfigurationLoader.getLogPath() + logname + ".log");
            logger.addHandler(fh);

            logger.info("[" + className + "] Start logging..");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}
