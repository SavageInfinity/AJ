/*
 * MIT License
 * Copyright (c) 2016 Estonian Information System Authority (RIA)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.dumonitor.storage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 */
public class Store extends HttpServlet {

  private static final int ERRCODE_10 = 10;
  private static final int ERRCODE_11 = 11;
  private static final int ERRCODE_9 = 9;
  private static final long serialVersionUID = 1L;

  // acceptable keys: identical to settable database fields
  public static String[] inKeys = {
      "personcode", "action", "sender", "receiver", "restrictions", "sendercode", "receivercode", "actioncode",
      "xroadrequestid", "xroadservice", "usercode"
  };

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    handleRequest(req, resp, false);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    handleRequest(req, resp, true);
  }

  private void handleRequest(HttpServletRequest req, HttpServletResponse resp, boolean isPost)
      throws ServletException, IOException {
    Context context = null;
    try {
      context = Util.initRequest(req, resp, "application/json; charset=UTF-8", Store.class);
      if (context == null)
        return;
      boolean ok = Util.parseInput(req, resp, context, inKeys, isPost);
      if (!ok || context.inParams == null)
        return;
      handleStoreParams(context);
    } catch (Exception e) {
      Util.showError(context, ERRCODE_9, "unexpected error: " + e.getMessage());
    }
    context.w.flush();
    context.w.close();
  }

  /**
   * Store parsed parameters passed as hashmap
   * @param context Request context
   * @throws ServletException generic catchall
   * @throws IOException  generic catchall
   */
  public void handleStoreParams(Context context) throws ServletException, IOException {
    Connection conn = Util.createDbConnection(context);
    if (conn == null)
      return;

    // insert data

    try {
      String sql = "insert into ajlog" + "(personcode,action," + "sender,receiver,restrictions,sendercode,receivercode,"
          + "actioncode,xroadrequestid,xroadservice,usercode)" + " values " + "(?,?, ?,?,?,?,?, ?,?,?,?)";
      PreparedStatement preparedStatement = conn.prepareStatement(sql);
      for (int i = 0; i < inKeys.length; i++) {
        if (context.inParams.get(inKeys[i]) != null) {
          preparedStatement.setString(i + 1, context.inParams.get(inKeys[i]));
        } else {
          preparedStatement.setNull(i + 1, java.sql.Types.VARCHAR);
        }
      }

      // execute insert SQL stetement
      int n = preparedStatement.executeUpdate();
      if (n != 1) {
        Util.showError(context, ERRCODE_11, "record not stored");
      } else {
        Util.showOK(context);
      }

    } catch (Exception e) {
      Util.showError(context, ERRCODE_10, "database storage error: " + e.getMessage());
      return;
    } finally {
      // It's important to close the connection when you are done with it
      try {
        conn.close();
      } catch (Throwable ignore) {
        Util.showError(context, ERRCODE_9, "cannot close database ");
      }
    }
  }

}
