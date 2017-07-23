package cn.windmourn.operator.utils;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class ConfigManager {

    private Configuration config;
    private File configFile;

    private ConfigManager(File file) {
        configFile = file;
        init();
        config.save();
    }

    public static ConfigManager loadConfig(File file) {
        return new ConfigManager(file);
    }

    public Configuration getConfig() {
        return config;
    }

    public void reloadConfig() {
        config = new Configuration(configFile);
        init();
        config.save();
    }

    private void init() {
        getOpsOwnerOnStart();
        getRegisterExtraCommands();
        getNeverExplosion();
        getExplosionNoBreak();
    }

    public boolean getOpsOwnerOnStart() {
        return config.get(Configuration.CATEGORY_GENERAL, "OpsOwnerOnStart", true, "Make yourself be a Operator every server start. Default is true.").getBoolean();
    }

    public boolean getRegisterExtraCommands() {
        return config.get(Configuration.CATEGORY_GENERAL, "RegisterExtraCommands", true, "Rigister some commands like fly, vanish, etc. Default is true.").getBoolean();
    }

    public boolean getNeverExplosion() {
        return config.get(Configuration.CATEGORY_GENERAL, "NeverExplosion", false, "ALL entities explosion will be disabled. Default is false.").getBoolean();
    }

    public boolean getExplosionNoBreak() {
        return config.get(Configuration.CATEGORY_GENERAL, "ExplosionNoBreak", true, "Explosion will NEVER break a block. Default is true.").getBoolean();
    }

}
