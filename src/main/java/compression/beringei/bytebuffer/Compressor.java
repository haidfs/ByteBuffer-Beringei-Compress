package compression.beringei.bytebuffer;
import static java.lang.Math.abs;

public class Compressor {
    private int previousLeadingZeros = Integer.MAX_VALUE;
    private int previousTrailingZeros = 0;
    private long previousValue = 0;
    private long previousTimestamp = 0;
    private long previousDelta = 0;

    private long blockTimestamp = 0;

    public final static short FIRST_DELTA_BITS = 27;

    private Encoding[] timestampEncodings = new Encoding[4];

    private Write write;

    public Compressor(long timestamp, Write bbwrite) {
        timestampEncodings[0] = new Encoding(7, 2, 2);
        timestampEncodings[1] = new Encoding(9, 6, 3);
        timestampEncodings[2] = new Encoding(12, 14, 4);
        timestampEncodings[3] = new Encoding(32, 15, 4);

        blockTimestamp = timestamp;
        write = bbwrite;
        writeHeaderTimestamp(timestamp);
    }

    private void writeHeaderTimestamp(long timestamp) {
        write.writeBits(timestamp, 64);
    }

    public void writePair(long timestamp, long value) {
        if (previousTimestamp == 0) {
            writeFirstPair(timestamp, value);
        } else {
            compressTimestamp(timestamp);
            compressValue(value);
        }
    }

    private void writeFirstPair(long timestamp, long value) {
        previousTimestamp = timestamp;
        previousDelta = timestamp - blockTimestamp;
        previousValue = value;

        write.writeBits(previousDelta, FIRST_DELTA_BITS);
        write.writeBits(previousValue, 64);
    }

    private void compressTimestamp(long timestamp) {
        long delta = timestamp - previousTimestamp;
        long deltaOfDelta = delta - previousDelta;
        if (deltaOfDelta == 0) {
			previousTimestamp = timestamp;
            previousDelta = delta;
            write.writeBits(0, 1);
            return;
        }

        if (deltaOfDelta > 0) {
            // There are no zeros. Shift by one to fit in x number of bits
            deltaOfDelta--;
        }
        long absValue = abs(deltaOfDelta);
        for (int i = 0; i < 4; i++) {
            if (absValue < ((long) 1 << (timestampEncodings[i].bitsForValue - 1))) {
                write.writeBits(timestampEncodings[i].controlValue, timestampEncodings[i].controlValueBitLength);
                // Make this value between [0, 2^timestampEncodings[i].bitsForValue - 1]
                long encodedValue = deltaOfDelta + ((long) 1 << (timestampEncodings[i].bitsForValue - 1));
                write.writeBits(encodedValue, timestampEncodings[i].bitsForValue);
                break;
            }
        }
        previousTimestamp = timestamp;
        previousDelta = delta;

    }

    private void compressValue(long value) {
        long xorWithPrevius = previousValue ^ value;
        if (xorWithPrevius == 0) {
            write.writeBits(0, 1);
            previousValue = value;
            return;
        }

        write.writeBits(1, 1);

        int leadingZeros = Long.numberOfLeadingZeros(xorWithPrevius);
        int trailingZeros = Long.numberOfTrailingZeros(xorWithPrevius);

       /* if(leadingZeros >= 32) {
            leadingZeros = 31;
        }*/
        //write.writeBits(1, 1);
        if (leadingZeros >= previousLeadingZeros && trailingZeros >= previousTrailingZeros) {
            write.writeBits(0, 1);
            int previousBlockInformationSize =
                    64 - previousTrailingZeros - previousLeadingZeros;
            long blockValue = xorWithPrevius >> previousTrailingZeros;
            write.writeBits(blockValue, previousBlockInformationSize);
        } else {
            write.writeBits(1, 1);
            write.writeBits(leadingZeros, 5);

            int blockInformationSize = 64 - leadingZeros - trailingZeros;

            write.writeBits(blockInformationSize, 6);
            write.writeBits(xorWithPrevius >> trailingZeros, blockInformationSize);

            previousLeadingZeros = leadingZeros;
            previousTrailingZeros = trailingZeros;
        }
        previousValue = value;
    }

    public void writeMagicTail() {
        write.writeBits(0x0F, 4);
        write.writeBits(0xFFFFFFFF, 32);
        write.writeBits(0, 1);
        write.flush();
    }


}