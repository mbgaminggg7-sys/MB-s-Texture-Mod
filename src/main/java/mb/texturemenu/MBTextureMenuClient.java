package mb.texturemenu;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MBTextureMenuClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("MB Texture Menu");

    @Override
    public void onInitializeClient() {
        MBConfig.get(); // load config
        LOG.info("MB's Texture Menu loaded");
    }
}
