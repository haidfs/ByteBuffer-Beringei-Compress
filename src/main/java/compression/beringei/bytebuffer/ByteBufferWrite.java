package compression.beringei.bytebuffer;
import java.nio.ByteBuffer;

public class ByteBufferWrite implements compression.beringei.bytebuffer.Write {
   
    public static final int Allocation_Capacity = 4096;

    
    private ByteBuffer bb;
    private byte b;
    private int bitsAvailable = Byte.SIZE;

    public ByteBufferWrite(){
        this(Allocation_Capacity);
    }

    public ByteBufferWrite(int capacity){
        bb = ByteBuffer.allocateDirect(capacity);
        b = bb.get(bb.position());
    }

    private void expandCapacity(){
        ByteBuffer expandedBB = ByteBuffer.allocateDirect(bb.capacity()*2);
        bb.flip();
        expandedBB.put(bb);
        expandedBB.position(bb.capacity());
        bb = expandedBB;
    }

    public void writeIntoByteBuffer(){
        if(bitsAvailable==0){
            bb.put(b);
            if(!bb.hasRemaining()){
                expandCapacity();
            }
            b = bb.get(bb.position());
            bitsAvailable =Byte.SIZE;
        }
    }
  
    public void writeBits(long value, int bitsInValue){
        while (bitsInValue > 0){
            int shift = bitsInValue - bitsAvailable;
            if (shift >= 0){
                b |= (byte)((value >> shift)&((1<<bitsAvailable) -1));
                bitsInValue -=bitsAvailable;
                bitsAvailable =0;
            } else{
                shift = bitsAvailable - bitsInValue;
                b |= (byte)(value<<shift);
                bitsAvailable -= bitsInValue;
                bitsInValue = 0;
            }
            writeIntoByteBuffer();
        }
    }

    public void flush() {
        bitsAvailable =0;
        writeIntoByteBuffer();
    }

    public ByteBuffer getByteBuffer() {
        return this.bb;
    }
}