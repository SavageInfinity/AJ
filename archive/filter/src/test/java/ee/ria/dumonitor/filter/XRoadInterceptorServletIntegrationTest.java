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
package ee.ria.dumonitor.filter;

import ee.ria.dumonitor.common.config.Property;
import ee.ria.dumonitor.common.util.ResourceUtil;
import ee.ria.testutils.jetty.EmbeddedJettyHttpServer;
import ee.ria.testutils.jetty.EmbeddedJettyIntegrationTest;
import ee.ria.testutils.servlet.MirroringServlet;
import ee.ria.testutils.soap.SoapTestUtil.XmlElement;

import org.junit.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import static ee.ria.testutils.soap.SoapTestUtil.createMessage;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class XRoadInterceptorServletIntegrationTest extends EmbeddedJettyIntegrationTest {

  private SOAPConnection connection;

  public XRoadInterceptorServletIntegrationTest() {
    super(new EmbeddedJettyHttpServer());
  }

  @Before
  public void setUp() throws Exception {
    createAndmekoguServlet();
    createLoggerServlet();
    createConnection();
  }

  private void createAndmekoguServlet() {
    createServlet(MirroringServlet.class, Property.ANDMEKOGU_URL.getURL().getPath());
  }

  private void createLoggerServlet() {
    createServlet(new HttpServlet() {
      @Override
      protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentLength(0);
      }
    }, Property.LOGGER_REST_URL.getURL().getPath());
  }

  private void createConnection() throws SOAPException {
    connection = SOAPConnectionFactory.newInstance().createConnection();
  }

  @After
  public void closeConnection() throws SOAPException {
    connection.close();
  }

  @Test
  public void testEmptyBody() throws SOAPException {
    SOAPMessage request = createMessage();
    SOAPMessage response = doCall(request);

    Iterator childElements = response.getSOAPBody().getChildElements();
    assertFalse(childElements.hasNext());
  }

  private SOAPMessage doCall(SOAPMessage request) throws SOAPException {
    String url = getTestWsUrl();
    return connection.call(request, url);
  }

  @Test
  public void testSingleElement() throws SOAPException {
    SOAPMessage request = createMessage(new XmlElement("testElement", "testElementValue"));
    SOAPMessage response = doCall(request);

    Node firstChild = response.getSOAPBody().getFirstChild();
    assertThat(firstChild.getNodeName(), is("testElement"));
    assertThat(firstChild.getTextContent(), is("testElementValue"));
  }

  @Test
  public void testSpecialCharacters() throws SOAPException {
    SOAPMessage request = createMessage(new XmlElement("õäöü", "šž"));
    SOAPMessage response = doCall(request);

    Node firstChild = response.getSOAPBody().getFirstChild();
    assertThat(firstChild.getNodeName(), is("õäöü"));
    assertThat(firstChild.getTextContent(), is("šž"));
  }

  @Test
  public void testAttachment() throws SOAPException {
    SOAPMessage request = createMessage(new XmlElement("testElement", "testElementValue"));

    URL attachmentUrl = ResourceUtil.getClasspathResource("test_attachment.jpg");
    AttachmentPart originalAttachment = request.createAttachmentPart(new DataHandler(attachmentUrl));
    originalAttachment.setContentId("test_attachment");
    request.addAttachmentPart(originalAttachment);

    SOAPMessage response = doCall(request);

    Iterator attachments = response.getAttachments();
    assertTrue(attachments.hasNext());

    AttachmentPart responseAttachment = (AttachmentPart) attachments.next();
    assertThat(responseAttachment.getContentId(), is(originalAttachment.getContentId()));
    assertThat(responseAttachment.getSize(), is(originalAttachment.getSize()));
  }

  @Test
  public void testLargeRequest() throws SOAPException {
    int numElements = 10000;

    XmlElement[] elements = new XmlElement[numElements];
    for (int i = 0; i < numElements; i++) {
      elements[i] = new XmlElement("element" + i, "value" + i);
    }

    SOAPMessage request = createMessage(elements);
    SOAPMessage response = doCall(request);

    NodeList childNodes = response.getSOAPBody().getChildNodes();
    assertThat(childNodes.getLength(), is(numElements));
  }

  @Test
  public void testConcurrentRequests() throws InterruptedException, ExecutionException, SOAPException {
    int numThreads = 10;
    int numRequests = 1000;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    List<Callable<SOAPMessage>> tasks = new ArrayList<Callable<SOAPMessage>>(numRequests);
    for (int i = 0; i < numRequests; i++) {
      tasks.add(new Callable<SOAPMessage>() {
        @Override
        public SOAPMessage call() throws Exception {
          return doCall(createMessage(new XmlElement("test", "test")));
        }
      });
    }

    List<Future<SOAPMessage>> futureResponses = executor.invokeAll(tasks);
    executor.shutdown();

    if (executor.awaitTermination(10, TimeUnit.SECONDS)) {
      for (Future<SOAPMessage> futureResponse : futureResponses) {
        SOAPBody body = futureResponse.get().getSOAPBody();
        assertThat(body.getFirstChild().getNodeName(), is("test"));
        assertThat(body.getFirstChild().getTextContent(), is("test"));
      }
    } else {
      executor.shutdownNow();
      fail();
    }
  }

  private String getTestWsUrl() {
    return getApplicationUrl() + Property.ANDMEKOGU_INTERCEPTOR_PATH.getValue();
  }

}
