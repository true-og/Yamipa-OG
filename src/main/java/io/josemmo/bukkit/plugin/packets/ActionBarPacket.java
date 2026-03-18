package io.josemmo.bukkit.plugin.packets;

import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import io.josemmo.bukkit.plugin.utils.Internals;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.trueog.utilitiesog.UtilitiesOG;

public class ActionBarPacket extends PacketContainer {

    private static final long serialVersionUID = 1L;
    private static final boolean USE_TITLE = (Internals.MINECRAFT_VERSION < 17);

    @SuppressWarnings("deprecation")
    public ActionBarPacket() {

        super(USE_TITLE ? PacketType.Play.Server.TITLE : PacketType.Play.Server.SET_ACTION_BAR_TEXT);
        if (USE_TITLE) {

            getTitleActions().write(0, EnumWrappers.TitleAction.ACTIONBAR);

        }

    }

    public @NotNull ActionBarPacket setText(@NotNull String text) {

        final String serialized = GsonComponentSerializer.gson().serialize(UtilitiesOG.trueogColorize(text));
        getChatComponents().write(0, WrappedChatComponent.fromJson(serialized));
        return this;

    }

}