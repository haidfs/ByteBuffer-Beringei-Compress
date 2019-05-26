package compression.beringei.bytebuffer;
public interface Read{
    //read 0 bit,read 1 bit or read many bits
    long readBits(int bitsInValue);
    // Reads a value until the first zero bit is found or limit reached.
    // The zero is included in the value as the least significant bit.
    int readValueThroughFirstZero(int limit);
}
