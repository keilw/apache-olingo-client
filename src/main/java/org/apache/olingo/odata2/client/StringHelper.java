/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olingo.odata2.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 *
 * @author michael
 */
public class StringHelper {

  public static String inputStreamToString(InputStream in) throws IOException {
    final StringBuilder stringBuilder = new StringBuilder();

    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")))) {
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line);
      }
    }

    return stringBuilder.toString();
  }
}