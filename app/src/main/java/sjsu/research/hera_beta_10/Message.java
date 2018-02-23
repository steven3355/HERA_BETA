package sjsu.research.hera_beta_10;

/**
 * Created by Steven on 2/7/2018.
 */

public class Message {
    private String _destination;
    private byte[] _data;
    public Message(byte[] data) {
        _data = data;
    }

    public byte[] getByte() {
        return _data;
    }

    public String getDestination() {
        return _destination;
    }
}
