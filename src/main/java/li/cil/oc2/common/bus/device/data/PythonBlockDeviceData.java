package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public final class PythonBlockDeviceData extends ForgeRegistryEntry<BlockDeviceData> implements BlockDeviceData {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final ByteBufferBlockDevice INSTANCE;

    private static InputStream getRootFilesystem() {
        InputStream stream = PythonBlockDeviceData.class.getClassLoader()
                .getResourceAsStream("/images/python.ext2");
        if (stream == null) {
            throw new IllegalStateException("no python image found");
        }
        return stream;
    }

    static {
        ByteBufferBlockDevice instance;
        try {
            instance = ByteBufferBlockDevice.createFromStream(getRootFilesystem(), true);
        } catch (final IOException e) {
            LOGGER.error(e);
            instance = ByteBufferBlockDevice.create(0, true);
        }
        INSTANCE = instance;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockDevice getBlockDevice() {
        return INSTANCE;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent("Python");
    }
}
