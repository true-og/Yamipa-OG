package io.josemmo.bukkit.plugin.packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Rotation;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import io.josemmo.bukkit.plugin.utils.Internals;

public class EntityMetadataPacket extends PacketContainer {

    private static final long serialVersionUID = 1L;
    private static final boolean USE_DATA_WATCHER;
    private static final int ITEM_INDEX;
    private static final int ROTATION_INDEX;
    private final WrappedDataWatcher dataWatcher = new WrappedDataWatcher(); // For <= 1.19.2
    private final List<WrappedDataValue> values = new ArrayList<>(); // For >= 1.19.3

    static {

        USE_DATA_WATCHER = (Internals.MINECRAFT_VERSION < 19.3f);
        ITEM_INDEX = (Internals.MINECRAFT_VERSION < 17) ? 7 : 8;
        ROTATION_INDEX = ITEM_INDEX + 1;

    }

    public EntityMetadataPacket() {

        super(PacketType.Play.Server.ENTITY_METADATA);

    }

    public @NotNull EntityMetadataPacket setId(int id) {

        getIntegers().write(0, id);
        return this;

    }

    public @NotNull EntityMetadataPacket setFlags(byte flags) {

        final WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Byte.class);
        if (USE_DATA_WATCHER) {

            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, serializer), flags);

        } else {

            values.add(new WrappedDataValue(0, serializer, flags));

        }

        return this;

    }

    public @NotNull EntityMetadataPacket setInvisible(boolean invisible) {

        final int flags = invisible ? 0x20 : 0x00;
        return setFlags((byte) flags);

    }

    public @NotNull EntityMetadataPacket setItem(@NotNull ItemStack item) {

        final WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.getItemStackSerializer(false);
        if (USE_DATA_WATCHER) {

            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(ITEM_INDEX, serializer), item);

        } else {

            values.add(new WrappedDataValue(ITEM_INDEX, serializer,
                    BukkitConverters.getItemStackConverter().getGeneric(item)));

        }

        return this;

    }

    public @NotNull EntityMetadataPacket setRotation(@NotNull Rotation rotation) {

        final WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(Integer.class);
        if (USE_DATA_WATCHER) {

            dataWatcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(ROTATION_INDEX, serializer),
                    rotation.ordinal());

        } else {

            values.add(new WrappedDataValue(ROTATION_INDEX, serializer, rotation.ordinal()));

        }

        return this;

    }

    public @NotNull EntityMetadataPacket build() {

        if (USE_DATA_WATCHER) {

            getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());

        } else {

            getDataValueCollectionModifier().write(0, values);

        }

        return this;

    }

}