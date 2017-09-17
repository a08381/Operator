package cn.windmourn.operator;

import net.minecraft.launchwrapper.network.protocol.ComponentEntity;
import net.minecraft.launchwrapper.network.socket.NetworkSocket;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileFilter;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions({"cn.windmourn.operator", "com.netease.mc.mod"})
@IFMLLoadingPlugin.MCVersion("1.11.2")
public class LoaderTweaker implements IFMLLoadingPlugin {

    public LoaderTweaker() {
        Logger logger = LogManager.getLogger("operator");
        for (ComponentEntity entity : NetworkSocket.componentKeys) {
            logger.info(entity.toString());
        }
        File folder = new File("mods");
        File[] files = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                for (ComponentEntity entity : NetworkSocket.componentKeys) {
                    if (entity.name.equals(pathname.getName())) {
                        return false;
                    }
                }
                return true;
            }
        });
        for (File file : files) {
            ComponentEntity entity = new ComponentEntity();
            entity.name = file.getName();
            logger.info(entity.toString());
            NetworkSocket.componentKeys.add(entity);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{MinecraftTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
