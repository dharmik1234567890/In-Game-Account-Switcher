/*
 * In-Game Account Switcher is a mod for Minecraft that allows you to change your logged in account in-game, without restarting Minecraft.
 * Copyright (C) 2015-2022 The_Fireplace
 * Copyright (C) 2021-2023 VidTu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package ru.vidtu.ias;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import org.lwjgl.glfw.GLFW;
import ru.vidtu.ias.account.Account;
import ru.vidtu.ias.mixins.MinecraftAccessor;
import ru.vidtu.ias.screen.AccountsScreen;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main IAS class for Fabric.
 *
 * @author VidTu
 */
@Environment(EnvType.CLIENT)
public final class IASFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize the IAS.
        IAS.init(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());

        // Set the UA.
        String modVersion = FabricLoader.getInstance().getModContainer("ias")
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getVersion)
                .map(Version::getFriendlyString)
                .orElse("UNKNOWN");
        String loaderVersion = FabricLoader.getInstance().getModContainer("fabricloader")
                .map(ModContainer::getMetadata)
                .map(ModMetadata::getVersion)
                .map(Version::getFriendlyString)
                .orElse("UNKNOWN");
        IAS.userAgent(modVersion, "Fabric", loaderVersion, SharedConstants.getCurrentVersion().getName());

        // Register closer.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> IAS.close(FabricLoader.getInstance().getGameDir()));

        // Debug.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (GLFW.glfwGetKey(client.getWindow().getWindow(), GLFW.GLFW_KEY_0) == GLFW.GLFW_PRESS) {
                client.setScreen(new AccountsScreen(null));
            }
        });
    }

    /**
     * Logins into the minecraft.
     * Can be called from any thread.
     *
     * @param minecraft Minecraft instance
     * @param data      Login data
     * @return Future for logging in
     */
    public static CompletableFuture<Void> account(Minecraft minecraft, Account.LoginData data) {
        // Create the accessor.
        MinecraftAccessor accessor = (MinecraftAccessor) minecraft;

        // Create everything async, because it lags.
        return CompletableFuture.runAsync(() -> {
            // Create the user.
            // I have no idea what are the OPTIONAL fields and the game
            // works FINE without them, even with chat reporting and parential control, etc.
            // etc., it may be some telemetry, it may be something else. If something is broken by this
            // feel free to submit an issue, if someone knows what this is, feel free to PR a fix.
            User.Type type = Objects.requireNonNullElse(User.Type.byName(data.type()), User.Type.LEGACY);
            User user = new User(data.name(), data.uuid(), data.token(), Optional.empty(), Optional.empty(), type);

            // Create various services.
            UserApiService apiService = accessor.ias$authenticationService().createUserApiService(data.token());
            CompletableFuture<UserApiService.UserProperties> properties = CompletableFuture.supplyAsync(() -> {
                try {
                    return apiService.fetchProperties();
                } catch (AuthenticationException ignored) {
                    return UserApiService.OFFLINE_PROPERTIES;
                }
            }, IAS.executor());
            PlayerSocialManager social = new PlayerSocialManager(minecraft, apiService);
            ClientTelemetryManager telemetry = new ClientTelemetryManager(minecraft, apiService, user);
            ProfileKeyPairManager keyPair = ProfileKeyPairManager.create(apiService, user, minecraft.gameDirectory.toPath());
            ReportingContext reporting = ReportingContext.create(ReportEnvironment.local(), apiService);

            // Schedule to the main thread
            minecraft.execute(() -> {
                // Flush everything.
                accessor.ias$user(user);
                accessor.ias$userApiService(apiService);
                accessor.ias$userPropertiesFuture(properties);
                accessor.ias$playerSocialManager(social);
                accessor.ias$telemetryManager(telemetry);
                accessor.ias$profileKeyPairManager(keyPair);
                accessor.ias$reportingContext(reporting);
            });
        }, IAS.executor()).exceptionally(t -> {
            // Log it.
            IAS.LOG.error("IAS: Unable to log in as {}/{}.", data.name(), data.uuid(), t);

            // Rethrow.
            throw new RuntimeException("Unable to change account to: " + data.name() + "/" + data.uuid(), t);
        });
    }
}
