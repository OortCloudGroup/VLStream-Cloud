import org.springblade.core.tool.utils.SM2Util;
import org.bouncycastle.util.encoders.Hex;

/**
 * EncryptHelper utility to encrypt a plaintext password using the default SM2 public key
 */
public class EncryptHelper {
    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "123456";
        String publicKey = "049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64";
        
        System.out.println("Plaintext Password: " + password);
        try {
            byte[] encrypted = SM2Util.encrypt(password, publicKey);
            String hexEncrypted = Hex.toHexString(encrypted);
            System.out.println("Encrypted SM2 Hex: " + hexEncrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
