public class TestApp {
    public static void main(String[] args) throws Exception {
        License license = LicenseUtil.generateLicense(360L);



        var licenseString = LicenseUtil.generateLicenseString(license);

        var test = LicenseUtil.getLicenseFromString(licenseString);

        var id = test.getId();//идем в бд, просим приватный ключ

        var privateKey = license.getPrivateKey();//типа залезли в бд


        System.out.println(LicenseUtil.isLicenseCorrect(test, privateKey));

    }
}
