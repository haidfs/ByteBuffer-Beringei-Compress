package compression.beringei.bytebuffer;
public class Decompressor{
    private int previousLeadingZeros = Integer.MAX_VALUE;
    private int previousTrailingZeros = 0;
    private long previousValue = 0;
    private long previousTimestamp = 0;
    private long previousDelta = 0;

    private Encoding[] timestampEncodings = new Encoding[4];

    private long blockTimestamp = 0;
    private boolean isMagicTail = false;

    private Read read;

    public Decompressor(Read bbread){
        timestampEncodings[0] = new Encoding(7, 2, 2);
        timestampEncodings[1] = new Encoding(9, 6, 3);
        timestampEncodings[2] = new Encoding(12, 14, 4);
        timestampEncodings[3] = new Encoding(32, 15, 4);

        read = bbread;
        readHeaderTimestamp();
    }

    private void readHeaderTimestamp(){
        blockTimestamp = read.readBits(64);
    }
    public Pair readPair(){
        if (previousTimestamp ==0){
            readFirstPair();
        }else {
            decompressTimestamp();
            decompressValue();
        }
        if(isMagicTail) {
            return null;
        }
        return new Pair(previousTimestamp,previousValue);
    }



    private void decompressTimestamp() {
        int type = findTheFirstZeroBit(4);
        if (type > 0) {
            // Delta of delta is non zero. Calculate the new delta. `index`
            // will be used to find the right length for the value that is
            // read.
            int index = type - 1;
            long decodedValue = read.readBits (timestampEncodings[index].bitsForValue);
            if ((int) decodedValue == 0xFFFFFFFF) {
                // End of stream
                isMagicTail = true;
                return;
            }
            // [0,255] becomes [-128,127]
            decodedValue -= ((long) 1 << (timestampEncodings[index].bitsForValue - 1));
            if (decodedValue >= 0) {
                // [-128,127] becomes [-128,128] without the zero in the middle
                decodedValue++;
            }
            previousDelta += decodedValue;
        }
        previousTimestamp += previousDelta;
    }

    private int findTheFirstZeroBit(int limit) {
        int bits = 0;
        while (bits < limit) {
            int bit = (int)read.readBits(1);
            if (bit == 0) {
                return bits;
            }
            bits++;
        }
        return bits;
    }


    private void decompressValue(){
        if ((int)read.readBits(1)==1) {
            // else -> same value as before
            if ((int)read.readBits(1)==1) {
                previousLeadingZeros = (int) read.readBits(5);

                byte significantBits = (byte) read.readBits(6);
                if(significantBits == 0) {
                    significantBits = 64;
                }
                previousTrailingZeros = 64 - significantBits - previousLeadingZeros;
            }
            long value = read.readBits(64 - previousLeadingZeros - previousTrailingZeros);
            value <<= previousTrailingZeros;
            value = previousValue ^ value;
            previousValue = value;
        }

    }

    private void readFirstPair(){
        previousDelta=read.readBits(Compressor.FIRST_DELTA_BITS);
        if(previousDelta == (1<<27) - 1) {
            isMagicTail = true;
            return;
        }
        previousValue=read.readBits(64);
        previousTimestamp=blockTimestamp+previousDelta;
    }

}