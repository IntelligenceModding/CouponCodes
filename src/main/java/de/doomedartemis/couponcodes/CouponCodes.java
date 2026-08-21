package de.doomedartemis.couponcodes;

import de.doomedartemis.couponcodes.common.command.CouponCommands;
import de.doomedartemis.couponcodes.common.advancement.AdvancementRewards;
import de.doomedartemis.couponcodes.common.advancement.CouponCriteria;
import de.doomedartemis.couponcodes.compat.curios.CuriosCompat;
import de.doomedartemis.couponcodes.common.config.CouponConfig;
import de.doomedartemis.couponcodes.common.coupon.CouponDailyBoost;
import de.doomedartemis.couponcodes.common.event.CouponBossBars;
import de.doomedartemis.couponcodes.common.event.CouponEvents;
import de.doomedartemis.couponcodes.common.loot.CouponLootDataManager;
import de.doomedartemis.couponcodes.common.network.ModNetwork;
import de.doomedartemis.couponcodes.common.registry.ModCreativeModeTabs;
import de.doomedartemis.couponcodes.common.registry.ModItems;
import de.doomedartemis.couponcodes.common.registry.ModLootModifiers;
import de.doomedartemis.couponcodes.common.registry.ModMenus;
import de.doomedartemis.couponcodes.common.registry.ModRecipeSerializers;
import de.doomedartemis.couponcodes.common.trade.CouponTradeDataManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(CouponCodes.MOD_ID)
public class CouponCodes {
    public static final String MOD_ID = "coupon_codes";

    public CouponCodes(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, CouponConfig.SPEC);
        modEventBus.addListener(ModNetwork::register);
        CuriosCompat.register(modEventBus);
        CouponCriteria.register(modEventBus);
        ModItems.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerEnchantItem);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onAnvilRepair);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onGrindstoneTakeItem);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onTradeWithVillager);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerBrewedPotion);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onArrowLoose);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onBonemeal);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onUseTotem);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onItemFished);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPickupXp);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onLivingFall);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(CouponEvents::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(CouponBossBars::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(CouponCommands::register);
        NeoForge.EVENT_BUS.addListener(CouponDailyBoost::onServerTick);
        NeoForge.EVENT_BUS.addListener(CouponLootDataManager::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(CouponTradeDataManager::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(CouponTradeDataManager::onWandererTrades);
        NeoForge.EVENT_BUS.addListener(CouponTradeDataManager::onVillagerTrades);
        NeoForge.EVENT_BUS.addListener(AdvancementRewards::onAdvancementEarned);
    }
}
