package util;

import config.ProxyConfig;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

// Utility class to load the configuration file
public class ConfigLoader {
    private static ProxyConfig INSTANCE;

    public static ProxyConfig load(String filePath) {
        if (INSTANCE == null) {
            try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(filePath)) {
                if (in == null) throw new RuntimeException("Config file not found: " + filePath);
                Yaml yaml = new Yaml(new Constructor(ProxyConfig.class, new LoaderOptions()));
                INSTANCE = yaml.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config: " + filePath, e);
            }
        }
        return INSTANCE;
    }

    public static ProxyConfig get() {
        if (INSTANCE == null) throw new RuntimeException("Config not loaded yet, call load(filePath) first");
        return INSTANCE;
    }
}

