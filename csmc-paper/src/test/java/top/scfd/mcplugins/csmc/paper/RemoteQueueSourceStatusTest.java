package top.scfd.mcplugins.csmc.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.GameMode;

final class RemoteQueueSourceStatusTest {
    @Test
    void totalsQueueSizeAcrossModes() {
        EnumMap<GameMode, Integer> sizes = new EnumMap<>(GameMode.class);
        sizes.put(GameMode.COMPETITIVE, 6);
        sizes.put(GameMode.WINGMAN, 2);

        RemoteQueueSourceStatus status = new RemoteQueueSourceStatus("srv-1", 4L, sizes);

        assertEquals(8, status.totalQueued());
    }

    @Test
    void handlesMissingQueueMapAsZero() {
        RemoteQueueSourceStatus status = new RemoteQueueSourceStatus("srv-2", 2L, Map.of());
        assertEquals(0, status.totalQueued());
    }
}
