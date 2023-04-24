import java.math.BigInteger;
public class Bounds {
    public BigInteger top, left, bottom, right;

    public Bounds(BigInteger top, BigInteger left, BigInteger bottom, BigInteger right) {
        this(top, left);
        this.bottom = bottom;
        this.right = right;
    }

    public Bounds(BigInteger top, BigInteger left) {
        this.top = top;
        this.left = left;
        this.bottom = top;
        this.right = left;
    }
}