/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olingo.odata2.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySetInfo;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;

public class ODataClient {

  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_XML = "application/xml";
  public static final String APPLICATION_ATOM_XML = "application/atom+xml";
  public static final String METADATA = "$metadata";
  //
  private String serviceUrl;
  private Proxy.Type protocol;
  private String proxyHostname;
  private int port;
  private boolean useProxy;
  private String username;
  private String password;
  private boolean useAuthentication;
  private Edm edm;

  private ODataClient() throws IOException, HttpException {
    this.serviceUrl = "";
    this.protocol = Proxy.Type.HTTP;
    this.port = 80;
    this.useProxy = false;
  }

  public ODataClient(String serviceUrl, Proxy.Type protocol, String proxy, int port) throws IOException, ODataException, HttpException {
    this.serviceUrl = serviceUrl;
    this.protocol = protocol;
    this.proxyHostname = proxy;
    this.port = port;
    this.useProxy = true;
    this.useAuthentication = false;

    edm = getEdmInternal();
  }

  public ODataClient(String serviceUrl, Proxy.Type protocol, String proxy, int port, String username, String password) throws IOException, ODataException, HttpException {
    this.serviceUrl = serviceUrl;
    this.protocol = protocol;
    this.proxyHostname = proxy;
    this.port = port;
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
    HttpURLConnection connection = connect(METADATA, APPLICATION_XML, "GET");
    edm = EntityProvider.readMetadata((InputStream) connection.getContent(), false);
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
    return EntityProvider.readFeed(contentType, entityContainer.getEntitySet(entitySetName), content, EntityProviderReadProperties.init().build());
  }

  public static InputStream getRawHttpEntity(String relativeUri, String contentType) throws HttpException, IOException {
    ODataClient client = new ODataClient();
    return (InputStream) client.connect(relativeUri, contentType, "GET").getContent();
  }

  private HttpURLConnection connect(String relativeUri, String contentType, String httpMethod) throws IOException, HttpException {
    URL url = new URL(serviceUrl + relativeUri);
    HttpURLConnection connection;
    if (useProxy) {
      Proxy proxy = new Proxy(protocol, new InetSocketAddress(proxyHostname, port));
      connection = (HttpURLConnection) url.openConnection(proxy);
    } else {
      connection = (HttpURLConnection) url.openConnection();
    }
    connection.setRequestMethod(httpMethod);
    connection.setRequestProperty("Accept", contentType);

    if (useAuthentication) {
      String authorization = "Basic ";
      authorization += new String(Base64.encodeBase64((username + ":" + password).getBytes()));
      connection.setRequestProperty("Authorization", authorization);
    }

    connection.connect();

    checkStatus(connection);

    return connection;
  }
}