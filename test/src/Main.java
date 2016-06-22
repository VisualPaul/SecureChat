import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.RSAPublicKeySpec;

/**
 * Created by paul on 22.06.16.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        KeyPair p = gen.generateKeyPair();
        System.out.println(p.getPublic().getEncoded());

    }
}
