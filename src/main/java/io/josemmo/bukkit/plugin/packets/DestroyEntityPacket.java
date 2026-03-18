package io.josemmo.bukkit.plugin.packets;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import io.josemmo.bukkit.plugin.utils.Internals;

public class DestroyEntityPacket extends PacketContainer {

    private static final long serialVersionUID = 1L;

    public DestroyEntityPacket() {

        super(PacketType.Play.Server.ENTITY_DESTROY);

    }

    public @NotNull DestroyEntityPacket setId(int id) {

        if (Internals.MINECRAFT_VERSION < 17) { // Minecraft 1.16.x

            getIntegerArrays().write(0, new int[] { id });

        } else if (Internals.MINECRAFT_VERSION < 17.1) { // Minecraft 1.17

            getIntegers().write(0, id);

        } else { // Minecraft 1.17.x

            getIntLists().write(0, Collections.singletonList(id));

        }

        return this;

    }

}