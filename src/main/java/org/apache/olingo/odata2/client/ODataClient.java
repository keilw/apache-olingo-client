/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olingo.odata2.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.odata2.api.commons.HttpHeaders;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntitySetInfo;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.apache.olingo.odata2.core.commons.ContentType;

public class ODataClient {

  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_XML = "application/xml";
  public static final String APPLICATION_ATOM_XML = "application/atom+xml";
  public static final String METADATA = "$metadata";
  //
  private String serviceUrl;
  private Proxy.Type proxyProtocol;
  private String proxyHostname;
  private int proxyPort;
  private boolean useProxy;
  private String username;
  private String password;
  private boolean useAuthentication;
  private Edm edm;

  private ODataClient() throws IOException, HttpException {
    this.serviceUrl = "";
    this.useProxy = false;
    this.useAuthentication = false;
  }

  public ODataClient(String serviceUrl) throws IOException, HttpException, ODataException {
    this.serviceUrl = serviceUrl;
    this.proxyProtocol = Proxy.Type.HTTP;
    this.proxyPort = 80;
    this.useProxy = false;

    edm = getEdmInternal();
  }

  public ODataClient(String serviceUrl, Proxy.Type protocol, String proxy, int port) throws IOException, ODataException, HttpException {
    this.serviceUrl = serviceUrl;
    this.proxyProtocol = protocol;
    this.proxyHostname = proxy;
    this.proxyPort = port;
    this.useProxy = true;
    this.useAuthentication = false;

    edm = getEdmInternal();
  }

  public ODataClient(String serviceUrl, Proxy.Type protocol, String proxy, int port, String username, String password)
          throws IOException, ODataException, HttpException {
    this.serviceUrl = serviceUrl;
    this.proxyProtocol = protocol;
    this.proxyHostname = proxy;
    this.proxyPort = port;
    this.useProxy = true;
    this.username = username;
    this.password = password;
    this.useAuthentication = true;

    edm = getEdmInternal();
  }

  public ODataClient(String serviceUrl, String username, String password) throws IOException, ODataException, HttpException {
    this.serviceUrl = serviceUrl;
    this.useProxy = false;
    this.username = username;
    this.password = password;
    this.useAuthentication = true;

    edm = getEdmInternal();
  }

  private Edm getEdmInternal() throws IOException, ODataException, HttpException {
    if (edm == null) {
      HttpURLConnection connection = connect(METADATA, APPLICATION_XML, "GET");
      edm = EntityProvider.readMetadata((InputStream) connection.getContent(), false);
    }
    return edm;
  }

  private void checkStatus(HttpURLConnection connection) throws IOException, HttpException {
    if (400 <= connection.getResponseCode() && connection.getResponseCode() <= 599) {
      HttpStatusCodes httpStatusCode = HttpStatusCodes.fromStatusCode(connection.getResponseCode());
      throw new HttpException(httpStatusCode, httpStatusCode.getStatusCode() + " " + httpStatusCode.toString());
    }
  }

  public Edm getEdm() {
    return edm;
  }

  public List<EdmEntitySetInfo> getEntitySets() throws ODataException {
    return edm.getServiceMetadata().getEntitySetInfos();
  }

  public ODataFeed readFeed(String entityContainerName, String entitySetName, String contentType) throws IOException, ODataException, HttpException {
    EdmEntityContainer entityContainer = edm.getEntityContainer(entityContainerName);
    String relativeUri;
    if (entityContainer.isDefaultEntityContainer()) {
      relativeUri = entitySetName;
    } else {
      relativeUri = entityContainerName + "." + entitySetName;
    }

    InputStream content = (InputStream) connect(relativeUri, contentType, "GET").getContent();
    content = storeRawContent(content);
    return EntityProvider.
            readFeed(contentType, entityContainer.getEntitySet(entitySetName), content, EntityProviderReadProperties.
                    init().build());
  }

  public void postEntity(String entitySetName, Map<String, Object> data) {
    String contentType = ContentType.APPLICATION_JSON.toContentTypeString();
    postEntity(entitySetName, data, contentType);
  }

  public void postEntity(String entitySetName, Map<String, Object> data, String contentType) {

    try {
      HttpURLConnection connection = initializeConnection(entitySetName, contentType, "POST");

      EdmEntitySet entitySet = getEntitySet(entitySetName);
      URI rootUri = new URI(entitySetName);

      EntityProviderWriteProperties properties = EntityProviderWriteProperties.serviceRoot(rootUri).build();
      ODataResponse response = EntityProvider.writeEntry(contentType, entitySet, data, properties);
      Object entity = response.getEntity();
      if (entity instanceof InputStream) {
        InputStream is = (InputStream) entity;
        byte[] buffer = new byte[2048];
        int size = is.read(buffer);

        connection.setDoOutput(true);
        //
        Logger.getLogger(ODataClient.class.getName()).log(Level.INFO, "\n" + new String(buffer, 0, size) + "\n");
        //
        connection.getOutputStream().write(buffer, 0, size);
      }

      connection.connect();

      storeRawContent(connection.getInputStream());
      checkStatus(connection);

      connection.disconnect();
    } catch (Exception ex) {
      Logger.getLogger(ODataClient.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private EdmEntitySet getEntitySet(String name) throws EdmException {
    List<EdmEntitySet> entitySets = edm.getEntitySets();
    for (EdmEntitySet edmEntitySet : entitySets) {
      if (edmEntitySet.getName().equals(name)) {
        return edmEntitySet;
      }
    }
    return null;
  }

  public static InputStream getRawHttpEntity(String relativeUri, String contentType) throws HttpException, IOException {
    ODataClient client = new ODataClient();
    return (InputStream) client.connect(relativeUri, contentType, "GET").getContent();
  }

  private HttpURLConnection connect(String relativeUri, String contentType, String httpMethod) throws IOException, HttpException {
    HttpURLConnection connection = initializeConnection(relativeUri, contentType, httpMethod);

    connection.connect();

    checkStatus(connection);

    return connection;
  }

  private HttpURLConnection initializeConnection(String relativeUri, String contentType, String httpMethod) throws MalformedURLException, IOException {
    URL url = new URL(serviceUrl + relativeUri);
    HttpURLConnection connection;
    if (useProxy) {
      Proxy proxy = new Proxy(proxyProtocol, new InetSocketAddress(proxyHostname, proxyPort));
      connection = (HttpURLConnection) url.openConnection(proxy);
    } else {
      connection = (HttpURLConnection) url.openConnection();
    }
    connection.setRequestMethod(httpMethod);
    connection.setRequestProperty("Accept", contentType);
    connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, contentType);

    if (useAuthentication) {
      String authorization = "Basic ";
      authorization += new String(Base64.encodeBase64((username + ":" + password).getBytes()));
      connection.setRequestProperty("Authorization", authorization);
    }

    return connection;
  }

  private String rawContentOfLastRequest;

  public String getRawContentOfLastRequest() {
    return rawContentOfLastRequest;
  }

  private InputStream storeRawContent(InputStream content) {
    try {
      String rawContent = StringHelper.inputStreamToString(content);
      rawContentOfLastRequest = rawContent;
      return new ByteArrayInputStream(rawContent.getBytes("UTF-8"));
    } catch (IOException ex) {
      Logger.getLogger(ODataClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return content;
  }
}
