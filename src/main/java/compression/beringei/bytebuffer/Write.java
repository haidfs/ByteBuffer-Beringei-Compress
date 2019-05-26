package compression.beringei.bytebuffer;
public interface Write{
    void writeBits(long value, int bitsInValue);
    void flush();
}