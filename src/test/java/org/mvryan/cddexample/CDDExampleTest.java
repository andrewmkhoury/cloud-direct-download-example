package org.mvryan.cddexample;

import static com.google.common.io.ByteStreams.toByteArray;
import com.google.common.io.Files;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import com.google.common.base.Strings;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;

import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CDDExampleTest {
    private static final String downloadName = "㌕ ㌖ ㌗ ㌘ ㌙ ㌚ ㌛ ㌜ ㌝.jpg";
    private static final String encodedDownloadName = "%E3%8C%95%20%E3%8C%96%20%E3%8C%97%20%E3%8C%98%20%E3%8C%99%20%E3%8C%9A%20%E3%8C%9B%20%E3%8C%9C%20%E3%8C%9D.jpg";
    private static final String contentType = "image/jpeg";

    private static BlobContainerClient blobContainerClient;

    @BeforeClass
    public static void setup() {
        Properties properties = getCfg();
        String accountName = properties.getProperty("accountName");
        String containerName = properties.getProperty("container");
        String accountKey = properties.getProperty("accountKey");

        blobContainerClient = new BlobServiceClientBuilder()
                .endpoint("https://" + accountName + ".blob.core.windows.net")
                .credential(new StorageSharedKeyCredential(accountName, accountKey))
                .buildClient()
                .getBlobContainerClient(containerName);
    }

    @Test
    public void testAzureDirectDownload() throws InvalidKeyException, IOException, IOException {
        assertNotNull(blobContainerClient);

        byte[] content = Files.toByteArray(new File("src/test/resources/Example.jpg"));
        String key = UUID.randomUUID().toString();
        uploadBlob(blobContainerClient, key, content);

        URL url = createSignedAzureBlobStorageDownloadURL(blobContainerClient, key);
        assertNotNull(url);

        System.out.println("URL: " + url);
        validateSignedUrl(url, content);

        blobContainerClient.getBlobClient(key).delete();
    }

    private void uploadBlob(BlobContainerClient containerClient, String blobName, byte[] content) {
        containerClient.getBlobClient(blobName).getBlockBlobClient().upload(new ByteArrayInputStream(content),
                content.length);
    }

    private void validateSignedUrl(@NotNull final URL url, @NotNull final byte[] expectedContent) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode());
        // get all headers
        java.util.Map<String, java.util.List<String>> map = conn.getHeaderFields();
        for (java.util.Map.Entry<String, java.util.List<String>> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        assertEquals(contentType, conn.getHeaderField("Content-Type"));
        assertEquals(getContentDispositionHeader(downloadName, encodedDownloadName), conn.getHeaderField("Content-Disposition"));

        assertTrue(java.util.Arrays.equals(expectedContent, toByteArray(conn.getInputStream())));
    }

    private URL createSignedAzureBlobStorageDownloadURL(BlobContainerClient containerClient, String blobName)
            throws InvalidKeyException, MalformedURLException {
        String contentDisposition = getContentDispositionHeader(downloadName, encodedDownloadName);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10);
        BlobContainerSasPermission blobContainerSasPermission = new BlobContainerSasPermission()
                .setReadPermission(true)
                .setWritePermission(true)
                .setListPermission(true);
        BlobServiceSasSignatureValues builder = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusDays(1),
                blobContainerSasPermission)
                .setProtocol(SasProtocol.HTTPS_ONLY)
                .setContentDisposition(contentDisposition)
                .setContentType(contentType);
        String sasToken = containerClient.generateSas(builder);
        return new URL(containerClient.getBlobClient(blobName).getBlobUrl() + "?" + sasToken);
    }

    private static Properties getCfg() {
        String cfg = "azure.properties";// System.getProperty(envKey);
        assertFalse(Strings.isNullOrEmpty(cfg));
        File cfgFile = new File(cfg);
        assertTrue(cfgFile.exists());
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(cfgFile)) {
            properties.load(in);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return properties;
    }

    private String getContentDispositionHeader(@NotNull final String downloadName,
            @NotNull final String encodedDownloadName) {
        return String.format("inline; filename*=UTF-8''%s",
                encodedDownloadName);
    }
}