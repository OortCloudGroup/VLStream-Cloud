import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.encoders.Hex;

public final class NativeSm2Password {
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected the VLS SM2 public key");
        }

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String password = input.readLine();
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Expected a password on standard input");
        }

        X9ECParameters curve = GMNamedCurves.getByName("sm2p256v1");
        ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
        ECPublicKeyParameters key = new ECPublicKeyParameters(curve.getCurve().decodePoint(Hex.decode(arguments[0])), domain);
        SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C2C3);
        engine.init(true, new ParametersWithRandom(key, new SecureRandom()));
        byte[] data = password.getBytes(StandardCharsets.UTF_8);
        System.out.println(Hex.toHexString(engine.processBlock(data, 0, data.length)));
    }
}
