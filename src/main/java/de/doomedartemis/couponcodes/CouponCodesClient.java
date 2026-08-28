package de.doomedartemis.couponcodes;

import de.doomedartemis.couponcodes.client.ClientConfig;
import de.doomedartemis.couponcodes.client.ClientModEvents;
import de.doomedartemis.couponcodes.client.SearchableConfigurationSectionScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CouponCodes.MOD_ID, dist = Dist.CLIENT)
public class CouponCodesClient {
    public CouponCodesClient(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new ConfigurationScreen(
                container,
                parent,
                (screen, type, config, title) -> new SearchableConfigurationSectionScreen(screen, type, config, title)
        ));
        modEventBus.addListener(ClientModEvents::onRegisterKeyMappings);
        modEventBus.addListener(ClientModEvents::onRegisterItemDecorations);
        modEventBus.addListener(ClientModEvents::onRegisterMenuScreens);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientModEvents::onBossEventProgress);
    }
}
