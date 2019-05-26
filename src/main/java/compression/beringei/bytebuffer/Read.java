package compression.beringei.bytebuffer;
public interface Read{
    //��0����1����ByteBuffer��һ�ζ����bit
    long readBits(int bitsInValue);
    // Reads a value until the first zero bit is found or limit reached.
    // The zero is included in the value as the least significant bit.
    int readValueThroughFirstZero(int limit);
}