package com.smartnsoft.droid4me.ws.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Test;

import android.util.Log;

import com.smartnsoft.droid4me.cache.FilePersistence;
import com.smartnsoft.droid4me.cache.Persistence;
import com.smartnsoft.droid4me.cache.Cacher.Status;
import com.smartnsoft.droid4me.cache.Persistence.PersistenceException;
import com.smartnsoft.droid4me.cache.Values.CacheException;
import com.smartnsoft.droid4me.cache.Values.CachingEvent;
import com.smartnsoft.droid4me.cache.Values.Info;
import com.smartnsoft.droid4me.cache.Values.Info.Source;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.ws.WebServiceCaller;
import com.smartnsoft.droid4me.ws.WebServiceClient;
import com.smartnsoft.droid4me.ws.WSUriStreamParser.UrlWithCallTypeAndBody;
import com.smartnsoft.droid4me.ws.WebServiceClient.CallType;
import com.smartnsoft.droid4me.wscache.BackedWSUriStreamParser;

/**
 * @author ɉdouard Mercier
 * @since 2011.08.16
 */
public final class Tests
{

  private final static class StreamParameter
  {

    private final long parameter;

    public StreamParameter(long parameter)
    {
      this.parameter = parameter;
    }

    public Map<String, String> computeUriParameters()
    {
      final Map<String, String> uriParameters = new HashMap<String, String>();
      uriParameters.put("parameter", Long.toString(parameter));
      return uriParameters;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (parameter ^ (parameter >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      StreamParameter other = (StreamParameter) obj;
      if (parameter != other.parameter)
        return false;
      return true;
    }

  }

  @SuppressWarnings("serial")
  private final static class TestException
      extends Exception
  {

    public TestException(Throwable throwable)
    {
      super(throwable);
    }

  }

  private static final String WEBSERVICES_BASE_URL = "http://www.google.com";

  @Before
  public void setup()
  {
    LoggerFactory.logLevel = Log.DEBUG;
    final File contentsDirectory = new File("tmp");
    Persistence.CACHE_DIRECTORY_PATHS = new String[] { contentsDirectory.getAbsolutePath() };
    // DbPersistence.FILE_NAMES = new String[] { DbPersistence.DEFAULT_FILE_NAME };
    // DbPersistence.TABLE_NAMES = new String[] { DbPersistence.DEFAULT_TABLE_NAME };
    FilePersistence.CACHE_FILE_COUNT_LIMITS = new int[] { Integer.MAX_VALUE };
    Persistence.INSTANCES_COUNT = 1;
    Persistence.IMPLEMENTATION_FQN = FilePersistence.class.getName();
    Persistence.clearAll();
  }

  @Test
  public void expiredRetention()
      throws CacheException, InterruptedException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    final StreamParameter parameter = new StreamParameter(System.currentTimeMillis());
    {
      streamParser.backed.getRetentionInfoValue(false, 1000000, null, parameter);
    }
    {
      Thread.sleep(200);
      getInputStreamCallsCount.set(0);
      final AtomicInteger onUriStreamParserCallCount = new AtomicInteger(0);
      final AtomicInteger onUriStreamParserStatusAttempt = new AtomicInteger(0);
      final AtomicInteger onUriStreamParserStatusSuccess = new AtomicInteger(0);
      final AtomicInteger onIOStreamerCallCount = new AtomicInteger(0);
      final AtomicInteger onIOStreamerStatusAttempt = new AtomicInteger(0);
      final AtomicInteger onIOStreamerStatusSuccess = new AtomicInteger(0);
      final Info<String> info = streamParser.backed.getRetentionInfoValue(true, 100, new CachingEvent()
      {

        public void onUriStreamParser(Status status)
        {
          onUriStreamParserCallCount.incrementAndGet();
          if (status == Status.Attempt)
          {
            onUriStreamParserStatusAttempt.incrementAndGet();
          }
          else if (status == Status.Success)
          {
            onUriStreamParserStatusSuccess.incrementAndGet();
          }
        }

        public void onIOStreamer(Status status)
        {
          onIOStreamerCallCount.incrementAndGet();
          if (status == Status.Attempt)
          {
            onIOStreamerStatusAttempt.incrementAndGet();
          }
          else if (status == Status.Success)
          {
            onIOStreamerStatusSuccess.incrementAndGet();
          }
        }

      }, parameter);
      Assert.assertEquals("The source of the data is not the right one", Source.URIStreamer, info.getSource());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, info.value);
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 1, getInputStreamCallsCount.get());
      Assert.assertEquals("'onUriStreamParser()' has not invoked the right number of times", 2, onUriStreamParserCallCount.get());
      Assert.assertEquals("'onUriStreamParser()' has not invoked invoked with the 'Attempt' status", 1, onUriStreamParserStatusAttempt.get());
      Assert.assertEquals("'onUriStreamParser()' has not invoked invoked with that 'Success' status", 1, onUriStreamParserStatusSuccess.get());
      Assert.assertEquals("'onIOStreamer()' has not invoked the right number of times", 0, onIOStreamerCallCount.get());
      Assert.assertEquals("'onIOStreamer()' has not invoked invoked with the 'Attempt' status", 0, onIOStreamerStatusAttempt.get());
      Assert.assertEquals("'onIOStreamer()' has not invoked invoked with that 'Success' status", 0, onIOStreamerStatusSuccess.get());
    }
  }

  @Test
  public void fromCacheRetention()
      throws CacheException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    final StreamParameter parameter = new StreamParameter(System.currentTimeMillis());
    {
      streamParser.backed.getRetentionInfoValue(false, 1000000, null, parameter);
    }
    {
      getInputStreamCallsCount.set(0);
      final AtomicInteger onUriStreamParserCallCount = new AtomicInteger(0);
      final AtomicInteger onIOStreamerCallCount = new AtomicInteger(0);
      final Info<String> info = streamParser.backed.getRetentionInfoValue(true, 1000000, new CachingEvent()
      {

        public void onUriStreamParser(Status status)
        {
          onUriStreamParserCallCount.incrementAndGet();
        }

        public void onIOStreamer(Status status)
        {
          onIOStreamerCallCount.incrementAndGet();
        }

      }, parameter);
      Assert.assertEquals("The source of the data is not the right one", Source.Memory, info.getSource());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, info.value);
      Assert.assertEquals("'onUriStreamParser()' has not invoked the right number of times", 0, onUriStreamParserCallCount.get());
      Assert.assertEquals("'onIOStreamer()' has not invoked the right number of times", 0, onIOStreamerCallCount.get());
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 0, getInputStreamCallsCount.get());
    }
  }

  @Test
  public void notFromCacheRetention()
      throws CacheException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    {
      final Info<String> info = streamParser.backed.getRetentionInfoValue(false, 1000000, null, new StreamParameter(System.currentTimeMillis()));
      Assert.assertEquals("The source of the data is not the right one", Source.URIStreamer, info.getSource());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, info.value);
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 1, getInputStreamCallsCount.get());
    }
  }

  @Test
  public void notFromCacheRetentionWhileAlreadyThere()
      throws CacheException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    final StreamParameter parameter = new StreamParameter(System.currentTimeMillis());
    {
      // We first retrieve the data by forcing a reload
      streamParser.backed.getRetentionInfoValue(false, 1000000, null, parameter);
    }

    {
      getInputStreamCallsCount.set(0);
      final AtomicInteger onUriStreamParserCallCount = new AtomicInteger(0);
      final AtomicInteger onUriStreamParserStatusAttempt = new AtomicInteger(0);
      final AtomicInteger onUriStreamParserStatusSuccess = new AtomicInteger(0);
      final AtomicInteger onIOStreamerCallCount = new AtomicInteger(0);
      final Info<String> info = streamParser.backed.getRetentionInfoValue(false, 1000000, new CachingEvent()
      {

        public void onUriStreamParser(Status status)
        {
          onUriStreamParserCallCount.incrementAndGet();
          if (status == Status.Attempt)
          {
            onUriStreamParserStatusAttempt.incrementAndGet();
          }
          else if (status == Status.Success)
          {
            onUriStreamParserStatusSuccess.incrementAndGet();
          }
        }

        public void onIOStreamer(Status status)
        {
          onIOStreamerCallCount.incrementAndGet();
        }

      }, parameter);
      Assert.assertEquals("The source of the data is not the right one", Source.URIStreamer, info.getSource());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, info.value);
      Assert.assertEquals("'onUriStreamParser()' has not invoked the right number of times", 2, onUriStreamParserCallCount.get());
      Assert.assertEquals("'onUriStreamParser()' has not invoked invoked with the 'Attempt' status", 1, onUriStreamParserStatusAttempt.get());
      Assert.assertEquals("'onUriStreamParser()' has not invoked invoked with that 'Success' status", 1, onUriStreamParserStatusSuccess.get());
      Assert.assertEquals("'onIOStreamer()' has not invoked the right number of times", 0, onIOStreamerCallCount.get());
      Assert.assertEquals("'getInputStream()' has been invoked the right number of times", 1, getInputStreamCallsCount.get());
    }
  }

  private WebServiceClient computeWebServiceClient(final AtomicInteger getInputStreamCallsCount, final String expectedValue)
  {
    final WebServiceClient webServiceClient = new WebServiceClient()
    {

      public String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
      {
        return WebServiceCaller.encodeUri(methodUriPrefix, methodUriSuffix, uriParameters, HTTP.UTF_8);
      }

      public InputStream getInputStream(String uri, CallType callType, HttpEntity body)
          throws CallException
      {
        getInputStreamCallsCount.incrementAndGet();
        return new ByteArrayInputStream(expectedValue.getBytes());
      }

    };
    return webServiceClient;
  }

  private BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> computeStreamParser(
      final WebServiceClient webServiceClient)
  {
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = new BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException>(Persistence.getInstance(0), webServiceClient)
    {

      public UrlWithCallTypeAndBody computeUri(StreamParameter parameters)
      {
        return new UrlWithCallTypeAndBody(webServiceClient.computeUri(Tests.WEBSERVICES_BASE_URL, "method", parameters.computeUriParameters()), CallType.Get, null);
      }

      public String parse(StreamParameter parameter, InputStream inputStream)
          throws TestException
      {
        try
        {
          return WebServiceCaller.getString(inputStream);
        }
        catch (IOException exception)
        {
          throw new TestException(exception);
        }
      }

    };
    return streamParser;
  }

}