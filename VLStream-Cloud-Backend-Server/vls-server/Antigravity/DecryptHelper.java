import org.springblade.modules.auth.utils.TokenUtil;

/**
 * DecryptHelper utility to decrypt the password sent in the curl request
 */
public class DecryptHelper {
    public static void main(String[] args) {
        String encryptedPassword = args.length > 0 ? args[0] : "ac0975008424e4a09253d1a5b8edc8cf340ac5e02593712fcb0484110030e4506b7c8790d896cea46816f263359b63990bd74c90da402fecb57e0d55f6edf5ca2f67c0557d6f63c7442bb42d3bbd14dabcee864f4322c789ac70c5ee4024d67b1a015cf7bfb0";
        String publicKey = "049787e408dea94acb3655acc5a7c7c7010bb9f140c84926c667ea616366082a118141c8dcb3e78a9d85d64fb765a250ff73448b18938f2219b94f782e28e1df64";
        String privateKey = "7204a0e7dd3f23783fb04a82b7ae211441b12a8cb42694f9997e02babc4bf65b";
        
        System.out.println("Input Ciphertext: " + encryptedPassword);
        try {
            // Ensure no leading 04 for slicing
            String cleanHex = encryptedPassword.startsWith("04") ? encryptedPassword.substring(2) : encryptedPassword;
            if (cleanHex.length() < 128 + 64) {
                System.out.println("Ciphertext too short!");
                return;
            }
            
            // Slice components
            String c1 = cleanHex.substring(0, 128);
            String rest = cleanHex.substring(128);
            int c3Len = 64;
            int c2Len = rest.length() - c3Len;
            
            System.out.println("Total length (no 04): " + cleanHex.length());
            System.out.println("Plaintext length: " + (c2Len / 2));
            
            // Format A: assuming input is C1C3C2 (mode 1), convert to C1C2C3 (mode 0)
            String c3_A = rest.substring(0, c3Len);
            String c2_A = rest.substring(c3Len);
            String formatA = "04" + c1 + c2_A + c3_A;
            
            // Format B: assuming input is C1C2C3 (mode 0), convert to C1C3C2 (mode 1)
            String c2_B = rest.substring(0, c2Len);
            String c3_B = rest.substring(c2Len);
            String formatB = "04" + c1 + c3_B + c2_B;
            
            // Try standard decryption
            String stdDec = org.springblade.core.tool.utils.SM2Util.decrypt("04" + cleanHex, privateKey);
            System.out.println("Standard Decryption (C1C2C3 or default): '" + stdDec + "'");
            
            // Try Format A decryption
            String decA = org.springblade.core.tool.utils.SM2Util.decrypt(formatA, privateKey);
            System.out.println("Format A (Converted from C1C3C2): '" + decA + "'");
            
            // Try Format B decryption
            String decB = org.springblade.core.tool.utils.SM2Util.decrypt(formatB, privateKey);
            System.out.println("Format B (Converted from C1C2C3): '" + decB + "'");
            
        } catch (Throwable e) {
            System.out.println("Decryption failed with exception:");
            e.printStackTrace();
        }
    }
}
