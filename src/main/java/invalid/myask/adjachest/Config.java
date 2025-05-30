package invalid.myask.adjachest;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean require_face_same = true;
    //public static boolean allow_doubling = true;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        require_face_same = configuration.getBoolean(
            "require_face_same",
            Configuration.CATEGORY_GENERAL,
            require_face_same,
            "Require chests already face same way to merge (as later vanilla)");

        /*
        allow_doubling = configuration.getBoolean(
            "allow_doubling",
            Configuration.CATEGORY_GENERAL,
            allow_doubling,
            "Allow chests to double up (false "
        );
         */

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
