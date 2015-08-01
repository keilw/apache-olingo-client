package org.apache.olingo.odata2.client.fxml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import de.mirb.util.io.StringHelper;
import de.mirb.util.web.HttpClient;
import de.mirb.util.web.HttpException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;

/**
 *
 */
public class BasicController implements Initializable {

  private static final String LB_CRLF = "CRLF";
  private static final String LB_CR = "CR";
  private static final String LB_LF = "LF";

  private enum HttpMethod {GET, PUT, POST, DELETE}

  private static final String CS_UTF_8 = "UTF-8";
  private static final String CS_ISO_8859_1 = "ISO-8859-1";

  @FXML ComboBox<String> uriSelector;
  @FXML TextArea rawResponseBody;
  @FXML TextArea rawResponseHeaders;
  @FXML WebView webView;
  @FXML TextArea logArea;
  @FXML TextArea requestHeaders;
  @FXML TextArea requestBody;
  @FXML ProgressIndicator progress;
  @FXML CheckBox proxyCheckbox;
  @FXML TextField proxyHost;
  @FXML TextField proxyPort;
  @FXML CheckBox loginCheckbox;
  @FXML TextField loginUser;
  @FXML PasswordField loginPassword;
  @FXML Button singleRequestButton;
  @FXML TabPane tabPane;
  @FXML ComboBox<HttpMethod> httpMethodCombo;
  @FXML ComboBox<String> lineBreakCombo;
  @FXML ComboBox<String> charsetCombo;

  private Task<Void> runningRequest = null;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    ObservableList<String> items = FXCollections.observableArrayList();
    items.add("http://www.olingo.org");
    items.add("http://localhost:8080/com.sap.core.odata.performance-web/ReferenceScenario.svc/");
    items.add("http://services.odata.org/Northwind/Northwind.svc/");
    items.add("http://localhost:8080/MyFormula.svc/");

    uriSelector.setItems(items);
    uriSelector.getSelectionModel().selectLast();
    //
    ObservableList<String> lineBreakOptions = FXCollections.observableArrayList();
    lineBreakOptions.add(LB_CRLF);
    lineBreakOptions.add(LB_CR);
    lineBreakOptions.add(LB_LF);
    lineBreakCombo.setItems(lineBreakOptions);
    lineBreakCombo.getSelectionModel().selectFirst();

    //
//    ObservableList<String> httpMethods = FXCollections.observableArrayList();
//    httpMethods.add(HTTP_METHOD_GET);
//    httpMethods.add(HTTP_METHOD_POST);
//    httpMethods.add(HTTP_METHOD_PUT);
//    httpMethods.add(HTTP_METHOD_DELETE);
    ObservableList<HttpMethod> httpMethods = FXCollections.observableArrayList();
    httpMethods.add(HttpMethod.GET);
    httpMethods.add(HttpMethod.POST);
    httpMethods.add(HttpMethod.PUT);
    httpMethods.add(HttpMethod.DELETE);

    httpMethodCombo.setItems(httpMethods);
    httpMethodCombo.getSelectionModel().selectFirst();

    ObservableList<String> charsets = FXCollections.observableArrayList();
    charsets.add(CS_UTF_8);
    charsets.add(CS_ISO_8859_1);

    charsetCombo.setItems(charsets);
    charsetCombo.getSelectionModel().selectFirst();
  }

  @FXML
  public void changeProxy(ActionEvent event) {
    if (proxyCheckbox.isSelected()) {
      proxyHost.setDisable(false);
      proxyPort.setDisable(false);
    } else {
      proxyHost.setDisable(true);
      proxyPort.setDisable(true);
    }
  }

  @FXML
  public void changeLogin(ActionEvent event) {
    if (loginCheckbox.isSelected()) {
      loginUser.setDisable(false);
      loginPassword.setDisable(false);
    } else {
      loginUser.setDisable(true);
      loginPassword.setDisable(true);
    }
  }

  
  @FXML
  public void sendSingleRequest(ActionEvent event) {
    if (runningRequest == null) {
      Task<Void> singleRequest = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          try {
            HttpClient.HttpClientBuilder clientBuilder = getClientBuilder();
            HttpClient.ClientResponse response = clientBuilder.execute();

            final String content = StringHelper.asString(response.getBody());
            final String headerContent = convertHeaders(response.getHeaders());

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                rawResponseBody.setText(content);
                rawResponseHeaders.setText(headerContent);
                webView.getEngine().loadContent(content);
                finishRunningRequest();
                singleRequestButton.setText("Single Request");
                // TODO: remove hard coded 2
                tabPane.getSelectionModel().select(2);
              }
            });

          } catch (IllegalArgumentException | IOException | HttpException ex) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                failRunningRequest(ex);
                singleRequestButton.setText("Single Request");
              }
            });
          }
          return null;
        }
      };

      clearViews();
      singleRequestButton.setText("Cancel");
      runningRequest = singleRequest;
      new Thread(singleRequest).start();
    } else {
      cancelRunningRequest();
      singleRequestButton.setText("Single Request");
    }

  }

  private String convertHeaders(Map<String, List<String>> headers) {
    StringBuilder content = new StringBuilder();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      List<String> value = entry.getValue();
      content.append("HI[").append(value.size()).append("] -> {")
          .append(entry.getKey())
          .append(": ").append(value).append("}\n");
    }
    return content.toString();
  }

  private void cancelRunningRequest() {
    if (runningRequest != null) {
      runningRequest.cancel();
      progress.setProgress(0);
      writeToLogArea("Canceled Running Request");
      runningRequest = null;
      enableRequestButtons();
    }
  }

  private void finishRunningRequest() {
    writeToLogArea("All requests successfully processed");
    progress.setProgress(1);
    runningRequest = null;
    enableRequestButtons();
  }

  private void failRunningRequest(Exception ex) {
    progress.setProgress(0);
    writeToLogArea(ex);
    runningRequest = null;
    enableRequestButtons();
  }

  private void enableRequestButtons() {
    singleRequestButton.setDisable(false);
  }

  private void clearViews() {
    logArea.setText("");
    webView.getEngine().loadContent("");
    rawResponseBody.setText("");
    progress.setProgress(-1);
  }

  private void writeToLogArea(Exception ex) {
    StringBuilder b = new StringBuilder();
    Writer out = new StringWriter();
    PrintWriter pw = new PrintWriter(out, true);
    ex.printStackTrace(pw);
    b.append("Exception occurred: ").append(ex.getMessage()).append("\n").append(out.toString());
    writeToLogArea(b.toString());
  }

  private void writeToLogArea(String content) {
    writeToLogArea(content, true);
  }

  private void writeToLogArea(String content, boolean append) {
    final StringBuilder b;
    if (append) {
      b = new StringBuilder(logArea.getText());
    } else {
      b = new StringBuilder();
    }
    b.append("--- ").append(DateFormat.getDateTimeInstance().format(new Date())).append("--------\n");
    b.append(content);
    b.append("\n--------------------------");
    logArea.setText(b.toString());
  }


  private String getValidUrl() throws IllegalArgumentException {
    String serviceUrl = uriSelector.getSelectionModel().getSelectedItem();
    if (isEmpty(serviceUrl)) {
      throw new IllegalArgumentException("No url given.");
    }
    return serviceUrl;
  }

  public HttpClient.HttpClientBuilder getClientBuilder() {
    HttpMethod httpMethod = httpMethodCombo.getSelectionModel().getSelectedItem();
    HttpClient.HttpClientBuilder builder = HttpClient.with(httpMethod.name(), getValidUrl());

    //headers
    String requestHeadersText = requestHeaders.getText();
    String[] reqHeaderLines = requestHeadersText.split("\r\n|\n|\r");
    for (String headerLine : reqHeaderLines) {
      String[] nameAndValue = headerLine.split(":");
      if(nameAndValue.length == 2) {
        builder.addHeader(nameAndValue[0], nameAndValue[1]);
      }
    }

    //
    if(HttpMethod.POST == httpMethod || HttpMethod.PUT == httpMethod) {
      String body = requestBody.getText();
      body = body.replaceAll("\n", getLineBreak());
      // TODO: replace hard coded utf-8
      String charset = charsetCombo.getSelectionModel().getSelectedItem();
      builder.setBody(new ByteArrayInputStream(body.getBytes(Charset.forName(charset))));
    }

    return builder;
  }

  private boolean isEmpty(String forValidation) {
    return forValidation == null || forValidation.length() == 0;
  }

  public String getLineBreak() {
    String lb = lineBreakCombo.getSelectionModel().getSelectedItem();
    switch (lb) {
    case LB_CRLF: return "\r\n";
    case LB_LF: return "\n";
    case LB_CR: return "\r";
    }
    return "\r\n";
  }
}
