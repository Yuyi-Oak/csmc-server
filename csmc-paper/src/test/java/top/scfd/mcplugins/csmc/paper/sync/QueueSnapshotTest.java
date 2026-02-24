package top.scfd.mcplugins.csmc.paper.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import top.scfd.mcplugins.csmc.api.GameMode;

final class QueueSnapshotTest {
    @Test
    void normalizesMissingAndNegativeValues() {
        EnumMap<GameMode, Integer> provided = new EnumMap<>(GameMode.class);
        provided.put(GameMode.COMPETITIVE, 7);
        provided.put(GameMode.CASUAL, -5);

        QueueSnapshot snapshot = new QueueSnapshot("server-a", 123L, provided);

        assertEquals(7, snapshot.queueSize(GameMode.COMPETITIVE));
        assertEquals(0, snapshot.queueSize(GameMode.CASUAL));
        assertEquals(0, snapshot.queueSize(GameMode.WINGMAN));
    }

    @Test
    void rejectsBlankServerId() {
        assertThrows(IllegalArgumentException.class, () -> new QueueSnapshot("  ", 1L, Map.of()));
    }

    @Test
    void clampsNegativeEpochSecond() {
        QueueSnapshot snapshot = new QueueSnapshot("server-b", -99L, Map.of());
        assertEquals(0L, snapshot.epochSecond());
    }
}
