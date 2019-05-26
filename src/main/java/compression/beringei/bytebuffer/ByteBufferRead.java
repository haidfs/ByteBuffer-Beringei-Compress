package compression.beringei.bytebuffer;
import java.nio.ByteBuffer;

public class ByteBufferRead implements compression.beringei.bytebuffer.Read {
    private ByteBuffer bb;
    private byte b;
    private int bitsAvailable = 0;

    public ByteBufferRead(ByteBuffer buf){
        bb = buf;
        readFromByteBuffer();
    }

    public ByteBufferRead(byte[] byteArray){
        this(ByteBuffer.wrap(byteArray));
    }


    public long readBits(int bitsInValue) {
        long value=0;
        while (bitsInValue > 0){
            if(bitsInValue>=bitsAvailable){
                byte lsb = (byte)(b & ((1<<bitsAvailable)-1));
                value = (value<<bitsAvailable) + (lsb & 0xff);
                bitsInValue -= bitsAvailable;
                bitsAvailable =0;
            }else {
                byte lsb =(byte)((b>>>(bitsAvailable-bitsInValue))&((1<<bitsInValue)-1));
                value = (value<<bitsInValue) + (lsb & 0xff);
                bitsAvailable -= bitsInValue;
                bitsInValue=0;
            }
            readFromByteBuffer();
        }
        return value;
    }

    public int readValueThroughFirstZero(int limit) {
        int value = 0;
        for (int bits = 0; bits < limit; bits++) {
            int bit = (int)readBits(1);
            value = (value << 1) + bit;
            if (bit == 0) {
                return value;
            }
        }
        return value;
    }

    private void readFromByteBuffer(){
        if (bitsAvailable == 0) {
            b = bb.get();
            bitsAvailable = Byte.SIZE;
        }
    }
}