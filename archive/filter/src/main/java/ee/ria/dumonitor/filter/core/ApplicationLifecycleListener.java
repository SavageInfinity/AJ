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
package ee.ria.dumonitor.filter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.ria.dumonitor.common.config.*;
import ee.ria.dumonitor.common.heartbeat.HeartbeatServlet;
import ee.ria.dumonitor.filter.XRoadInterceptorServlet;
import ee.ria.dumonitor.filter.config.ConfigurationLoader;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Date;

/**
 * Listens for changes to the servlet context. Actions related to application startup and shutdown should be taken in
 * this class.
 */
public class ApplicationLifecycleListener implements ServletContextListener {

  private static final Logger LOG = LoggerFactory.getLogger(ApplicationLifecycleListener.class);

  private static final PropertyHolder[] REQUIRED_PROPERTIES = {
      Property.FILTER_CONFIGURATION_FILE,
      Property.TURVASERVER_URL,
      Property.ANDMEKOGU_URL,
      Property.TURVASERVER_INTERCEPTOR_PATH,
      Property.ANDMEKOGU_INTERCEPTOR_PATH,
      Property.LOGGER_REST_URL,
      Property.HEARTBEAT_PATH,
      BuildProperty.BUILD_DATE,
      BuildProperty.NAME,
      BuildProperty.VERSION
  };

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    RuntimeProperty.APPLICATION_STARTUP_TIME.setValue(new Date());

    PropertyLoader.loadProperties(Property.class, "default.properties", "dumonitor.properties");
    PropertyLoader.loadProperties(BuildProperty.class, "build.properties");
    PropertyLoader.requireProperties(REQUIRED_PROPERTIES);

    ConfigurationLoader.loadConfiguration("filter-defaults.xml", Property.FILTER_CONFIGURATION_FILE.getValue());

    ServletInitializer servletInitializer = new ServletInitializer(sce.getServletContext());
    servletInitializer.addServlet(XRoadInterceptorServlet.class,
                                  Property.ANDMEKOGU_INTERCEPTOR_PATH.getValue(),
                                  Property.TURVASERVER_INTERCEPTOR_PATH.getValue());
    servletInitializer.addServlet(HeartbeatServlet.class, Property.HEARTBEAT_PATH.getValue());

    LOG.info("Application started");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    LOG.info("Stopping application");
    ExecutorManager.shutdownAll();
  }

}
