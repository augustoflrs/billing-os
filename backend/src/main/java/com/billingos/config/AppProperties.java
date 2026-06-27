package com.billingos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Minio minio = new Minio();
    private Jwt   jwt   = new Jwt();
    private Dte   dte   = new Dte();

    public Minio getMinio() { return minio; }
    public void  setMinio(Minio minio) { this.minio = minio; }
    public Jwt   getJwt()   { return jwt; }
    public void  setJwt(Jwt jwt)       { this.jwt = jwt; }
    public Dte   getDte()   { return dte; }
    public void  setDte(Dte dte)       { this.dte = dte; }

    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;

        public String getEndpoint()   { return endpoint; }
        public void setEndpoint(String v)   { this.endpoint = v; }
        public String getAccessKey()  { return accessKey; }
        public void setAccessKey(String v)  { this.accessKey = v; }
        public String getSecretKey()  { return secretKey; }
        public void setSecretKey(String v)  { this.secretKey = v; }
        public String getBucket()     { return bucket; }
        public void setBucket(String v)     { this.bucket = v; }
    }

    public static class Jwt {
        private String secret;
        private long   expirationMs;

        public String getSecret()        { return secret; }
        public void setSecret(String v)       { this.secret = v; }
        public long getExpirationMs()    { return expirationMs; }
        public void setExpirationMs(long v)   { this.expirationMs = v; }
    }

    public static class Dte {
        private String mhApiUrl;
        private int    maxRetryAttempts        = 5;
        private int    retryBaseDelaySeconds   = 30;
        private String transmissionCron        = "*/5 * * * * *";
        /** Password for the active DTE PKCS12 keystore. Set via DTE_CERTIFICATE_PASSWORD env var. */
        private String certificatePassword     = "";

        public String getMhApiUrl()              { return mhApiUrl; }
        public void setMhApiUrl(String v)             { this.mhApiUrl = v; }
        public int getMaxRetryAttempts()         { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int v)        { this.maxRetryAttempts = v; }
        public int getRetryBaseDelaySeconds()    { return retryBaseDelaySeconds; }
        public void setRetryBaseDelaySeconds(int v)   { this.retryBaseDelaySeconds = v; }
        public String getTransmissionCron()      { return transmissionCron; }
        public void setTransmissionCron(String v)     { this.transmissionCron = v; }
        public String getCertificatePassword()   { return certificatePassword; }
        public void setCertificatePassword(String v)  { this.certificatePassword = v; }
    }
}
