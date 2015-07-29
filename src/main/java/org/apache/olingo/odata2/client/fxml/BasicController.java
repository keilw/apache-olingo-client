package org.apache.olingo.odata2.client.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

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
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;

import org.apache.olingo.odata2.client.HttpException;
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;

/**
 *
 */
public class BasicController implements Initializable {

  @FXML ComboBox<String> uriSelector;
  @FXML TextArea rawView;
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
            ODataClient.ODataClientBuilder clientBuilder = getClientBuilder();
            InputStream is = clientBuilder.execute();
            final String content = StringHelper.inputStreamToString(is);

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                rawView.setText(content);
                webView.getEngine().loadContent(content);
                finishRunningRequest();
                singleRequestButton.setText("Single Request");
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
    writeToLogArea("All requests successfull processed");
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
    rawView.setText("");
    progress.setProgress(-1);
  }

  private void writeToLogArea(Exception ex) {
    StringBuilder b = new StringBuilder();
    Writer out = new StringWriter();
    PrintWriter pw = new PrintWriter(out, true);
    ex.printStackTrace(pw);
    b.append("Exception occured: ").append(ex.getMessage()).append("\n").append(out.toString());
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

  public ODataClient.ODataClientBuilder getClientBuilder() {
    ODataClient.ODataClientBuilder builder = ODataClient.get(getValidUrl());
    String requestHeadersText = requestHeaders.getText();
    String[] reqHeaderLines = requestHeadersText.split("\r\n|\n|\r");
    for (String headerLine : reqHeaderLines) {
      String[] nameAndValue = headerLine.split(":");
      if(nameAndValue.length == 2) {
        builder.addHeader(nameAndValue[0], nameAndValue[1]);
      }
    }
    return builder;
  }

  private boolean isEmpty(String forValidation) {
    return forValidation == null || forValidation.length() == 0;
  }
}
