import org.springblade.core.tool.utils.SM2Util;
import org.bouncycastle.util.encoders.Hex;

/**
 * Sm2FormatCheck to inspect Java SM2 encryption output format
 */
public class Sm2FormatCheck {
    public static void main(String[] args) {
        String password = "admin";
        String publicKey = "049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64";
        
        try {
            byte[] encrypted = SM2Util.encrypt(password, publicKey);
            String hexEnc = Hex.toHexString(encrypted);
            System.out.println("Java Encrypted Hex: " + hexEnc);
            System.out.println("Java Encrypted Length: " + hexEnc.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
