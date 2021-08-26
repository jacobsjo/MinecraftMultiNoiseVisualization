package eu.jacobsjo.multinoisevis;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.WorldVersion;
import net.minecraft.world.level.storage.DataVersion;

import java.util.Date;

public class DummyVersion implements WorldVersion {
    @Override
    public DataVersion getDataVersion() {
        return new DataVersion(999999, "dummy");
    }

    @Override
    public String getId() {
        return "dummy";
    }

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getReleaseTarget() {
        return "dummy";
    }

    @Override
    public int getProtocolVersion() {
        return 999999;
    }

    @Override
    public Date getBuildTime() {
        return null;
    }

    @Override
    public boolean isStable() {
        return false;
    }
}
