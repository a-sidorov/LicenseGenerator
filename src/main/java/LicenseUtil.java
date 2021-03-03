import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exception.LicenseKeyException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

public class LicenseUtil {

    private static KeyPairGenerator keyPairGenerator;
    private static Cipher cipher;
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    static {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    private static String encryptPublicKey(PublicKey publicKey) throws LicenseKeyException {
        var data = publicKey.getEncoded();

        try {
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            cipher.update(data, 0, data.length / 2);

            var licenseKey1 = cipher.doFinal();


            cipher.update(data, data.length / 2, data.length / 2);
            var licenseKey2 = cipher.doFinal();
            var licenseKey = ArrayUtils.addAll(licenseKey1, licenseKey2);

            return Base64.getEncoder().encodeToString(licenseKey);

        } catch (Exception e) {
            throw new LicenseKeyException("Public key generation err", e);
        }
    }

    private static PublicKey decryptPublicKey(String encodedKey, PrivateKey privateKey) throws LicenseKeyException {
        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            var licenseKey = Base64.getDecoder().decode(encodedKey);
            cipher.update(licenseKey, 0, licenseKey.length / 2);

            // Получаем public_key
            byte[] decoded1 = cipher.doFinal();
            cipher.update(licenseKey, licenseKey.length / 2, licenseKey.length / 2);
            byte[] decoded2 = cipher.doFinal();

            var decoded = ArrayUtils.addAll(decoded1, decoded2);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");


            return keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new LicenseKeyException("Public key decoding err", e);
        }

    }

    private static PrivateKey decodePrivateKetFromString(String encodedKey) throws LicenseKeyException {
        try {
            var decoded = Base64.getDecoder().decode(encodedKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return keyFactory.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new LicenseKeyException(e);
        }

    }


    public static License generateLicense(long duration, long userId) throws LicenseKeyException {

        keyPairGenerator.initialize(1024, new SecureRandom());

        var keyPair = keyPairGenerator.generateKeyPair();

        // В license_key будем хранить public_key и проверять его через шифровку-дешифровку
        var publicKey = keyPair.getPublic();
        var privateKey = keyPair.getPrivate();


        var licenseID = UUID.randomUUID();//Вот это пихаем в бд как прайм кей.

        var licenseKey = LicenseUtil.encryptPublicKey(publicKey);


        return new License(licenseID,
                Base64.getEncoder().encodeToString(privateKey.getEncoded()),
                licenseKey,
                Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now().plusDays(duration)), userId, "default");
    }

    // Этот метод проверяет лицензию на корректность, то есть проверяет только license_key
    // За актуальность лицензии ответственность должен нести другой класс/другой метод
    public static boolean isLicenseCorrect(PublicLicense publicLicense, String privateKeyString) throws LicenseKeyException {

        try {

            var privateKey = LicenseUtil.decodePrivateKetFromString(privateKeyString);

            var publicKey = LicenseUtil.decryptPublicKey(publicLicense.getLicenseKey(), privateKey);

            // Шифруем-дешифруем
            Cipher decode = Cipher.getInstance("RSA");
            Cipher encode = Cipher.getInstance("RSA");


            decode.init(Cipher.DECRYPT_MODE, privateKey);
            encode.init(Cipher.ENCRYPT_MODE, publicKey);

            int length = 100;
            boolean useLetters = true;
            boolean useNumbers = false;
            String generatedString = RandomStringUtils.random(length, useLetters, useNumbers);

            encode.update(generatedString.getBytes(StandardCharsets.UTF_8));
            decode.update(encode.doFinal());

            return generatedString.equals(new String(decode.doFinal(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new LicenseKeyException(e);
        }

    }

    // Этот метод генерирует из экземпляра лицензии строку для записи в файл лицензии
    public static String generateLicenseString(License license) {
        return Base64.getEncoder().encodeToString(gson.toJson(license).getBytes(StandardCharsets.UTF_8));
    }

    // Этот метод достает из строки лицензии экземпляр лицензии
    public static PublicLicense getLicenseFromString(String licenseString) {
        var encodedLicense = new String(Base64.getDecoder().decode(licenseString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        return gson.fromJson(encodedLicense, PublicLicense.class);
    }

}
