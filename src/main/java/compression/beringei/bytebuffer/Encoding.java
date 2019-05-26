package compression.beringei.bytebuffer;
public class Encoding{
    public int bitsForValue;
    public long controlValue;
    public int controlValueBitLength;
    public Encoding(int bitsForValue, long controlValue, int controlValueBitLength){
        this.bitsForValue =bitsForValue;
        this.controlValue = controlValue;
        this.controlValueBitLength = controlValueBitLength;
    }
}