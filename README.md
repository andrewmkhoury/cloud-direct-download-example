# Cloud Direct Download Example

This project gives a simple example of how to download a blob from cloud storage using signed URLs in which the Content-Type and Content-Disposition headers are specified.   Both Microsoft Azure Blob Storage and Amazon Web Services S3 cloud storage services are included in this example.

This project also showcases a reported issue for the Azure SDK for Java, issue [2900](https://github.com/Azure/azure-sdk-for-java/issues/2900), in which properly setting the Content-Disposition header for the response doesn't function as expected.

The project can be run simply from the command-line as follows:

`mvn clean test`

Be sure to set the "azure.config" and "s3.config" properties on the command-line.  These should each point to a simple properties file containing values needed to connect to cloud storage in order to execute the test.

For Azure, in `azure.properties`, the properties that need to be set are:
* **accountName** - The name of your Azure storage account.
* **accountKey** - A valid access key for your Azure storage account.
* **container** - The name of the container to store data in.  This container must exist before the test begins.

When the test executes, it does the same activity for both services:
* First, it uploads a blob from test/resources/Example.jpg.
* Next, a signed URL is generated for that blob, during which the values that should be returned for the Content-Type and Content-Disposition response headers are specified.
* Next, the code attempts to download the content in the blob, verifying that a successful HTTP response is received (200).
* If the download is successful, the test then verifies that the Content-Type and Content-Disposition headers in the response were set correctly.
* Finally, the test validates that the content is the same content that was originally stored.
* At the end of the test any blobs created are cleaned up.

At present, this code manifests the issue reported in Azure SDK for Java [2900](https://github.com/Azure/azure-sdk-for-java/issues/2900), which is that a properly formatted UTF-8 value specified for the response's Content-Disposition header results in a 400 response.
