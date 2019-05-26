package compression.beringei.bytebuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;


public class EncodeTest {

    private void comparePairsToCompression(long blockTimestamp, Pair[] pairs) {
        ByteBufferWrite output = new ByteBufferWrite();
        Compressor c = new Compressor(blockTimestamp, output);
        Arrays.stream(pairs).forEach(p -> c.writePair(p.getTimestamp(), p.getValue()));
        c.writeMagicTail();

        ByteBuffer byteBuffer = output.getByteBuffer();
        byteBuffer.flip();

        ByteBufferRead input = new ByteBufferRead(byteBuffer);
        Decompressor d = new Decompressor(input);

        // Replace with stream once decompressor supports it
        for(int i = 0; i < pairs.length; i++) {
            Pair pair = d.readPair();
            assertEquals(pairs[i].getTimestamp(), pair.getTimestamp(), "Timestamp did not match");
            assertEquals(pairs[i].getValue(), pair.getValue(), "Value did not match");
        }

        assertNull(d.readPair());
    }

    @Test
    void simpleEncodeAndDecodeTest() throws Exception {
        long now = 1500405481623L;

        Pair[] pairs = {
                new Pair(now + 10, Double.doubleToRawLongBits(1.0)),
               new Pair(now + 20, Double.doubleToRawLongBits(-2.0)),
                new Pair(now + 28, Double.doubleToRawLongBits(-2.5))
        };

        comparePairsToCompression(now, pairs);
    }

}