package cn.windmourn.operator;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.TransformerExclusions("cn.windmourn.operator")
@IFMLLoadingPlugin.MCVersion("1.11.2")
public class Loader implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"cn.windmourn.operator.Transformer"};
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
