package compression.beringei.bytebuffer;
public class Pair{
    private long timestamp;
    private long value;
    public Pair(long timestamp,long value){
        this.timestamp =timestamp;
        this.value = value;
    }
    public long getTimestamp(){
        return timestamp;
    }

    public long getValue(){
        return value;
    }
}