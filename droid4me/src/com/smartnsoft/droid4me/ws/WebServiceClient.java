/*
 * (C) Copyright 2009-2011 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.ws;

import java.io.InputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.http.HttpEntity;

/**
 * A minimalist contract when creating a web service client.
 * 
 * @author Édouard Mercier
 * @since 2011.08.17
 */
public interface WebServiceClient
{

  /**
   * An HTTP method type.
   */
  public static enum Verb
  {
    Get, Post, Put, Delete;
  }

  /**
   * Defines the way the HTTP method is run, and enables to link a call code with it.
   */
  public final static class CallType
  {

    /**
     * The call code when not defined.
     */
    public final static int NO_CALL_CODE = -1;

    public final static WebServiceClient.CallType Get = new WebServiceClient.CallType(WebServiceClient.Verb.Get, WebServiceClient.CallType.NO_CALL_CODE);

    public final static WebServiceClient.CallType Post = new WebServiceClient.CallType(WebServiceClient.Verb.Post, WebServiceClient.CallType.NO_CALL_CODE);

    public final static WebServiceClient.CallType Put = new WebServiceClient.CallType(WebServiceClient.Verb.Put, WebServiceClient.CallType.NO_CALL_CODE);

    public final static WebServiceClient.CallType Delete = new WebServiceClient.CallType(WebServiceClient.Verb.Delete, WebServiceClient.CallType.NO_CALL_CODE);

    /**
     * The HTTP method.
     */
    public final WebServiceClient.Verb verb;

    /**
     * A code that may associated with the call.
     */
    public final int callCode;

    public CallType(WebServiceClient.Verb verb, int callCode)
    {
      this.verb = verb;
      this.callCode = callCode;
    }

    @Override
    public String toString()
    {
      return verb.toString();
    }

  }

  /**
   * The exception that will be thrown if any problem occurs during a web service call.
   */
  public static class CallException
      extends Exception
  {

    private static final long serialVersionUID = 4869741128441615773L;

    private int statusCode;

    public CallException(String message, Throwable throwable)
    {
      super(message, throwable);
    }

    public CallException(String message)
    {
      super(message);
    }

    public CallException(Throwable throwable)
    {
      super(throwable);
    }

    public CallException(String message, int statusCode)
    {
      this(message, null, statusCode);
    }

    public CallException(Throwable throwable, int statusCode)
    {
      this(null, throwable, statusCode);
    }

    public CallException(String message, Throwable throwable, int statusCode)
    {
      super(message, throwable);
      this.statusCode = statusCode;
    }

    public int getStatusCode()
    {
      return statusCode;
    }

    /**
     * @return {@code true} is the current exception is linked to a connectivity problem with Internet.
     * @see #isConnectivityProblem(Throwable)
     */
    public final boolean isConnectivityProblem()
    {
      return WebServiceClient.CallException.isConnectivityProblem(this);
    }

    /**
     * Indicates whether the cause of the provided exception is due to a connectivity problem.
     * 
     * @param throwable
     *          the exception to test
     * @return {@code true} if the {@link Throwable} was triggered because of a connectivity problem with Internet
     */
    public static boolean isConnectivityProblem(Throwable throwable)
    {
      Throwable cause;
      Throwable newThrowable = throwable;
      // We investigate over the whole cause stack
      while ((cause = newThrowable.getCause()) != null)
      {
        if (cause instanceof UnknownHostException || cause instanceof SocketException)
        {
          return true;
        }
        newThrowable = cause;
      }
      return false;
    }

  }

  /**
   * Indicates the type of HTTP request and the underlying HTTP.
   * 
   * @since 2009.11.10
   */
  public final static class HttpCallTypeAndBody
  {

    /**
     * The actual URL of the HTTP request to execute.
     */
    public final String url;

    /**
     * The HTTP request method.
     */
    public final CallType callType;

    /**
     * If the HTTP method is a {@link Verb#Post} or a {@link Verb#Put}, the body of the request.
     */
    public final HttpEntity body;

    /**
     * This will create a {@link Verb#Get} HTTP request method.
     * 
     * @param url
     *          the URL to use when performing the HTTP request
     */
    public HttpCallTypeAndBody(String url)
    {
      this(url, CallType.Get, null);
    }

    /**
     * @param url
     *          the URL to use when performing the HTTP request
     * @param callType
     *          the HTTP request method
     * @param body
     *          the HTTP request body, if the 'callType" is a {@link Verb#Post POST} or a {@link Verb#Put PUT}
     */
    public HttpCallTypeAndBody(String url, CallType callType, HttpEntity body)
    {
      this.url = url;
      this.callType = callType;
      this.body = body;
    }

    @Override
    public String toString()
    {
      return "(" + callType + ") " + url;
    }

  }

  /**
   * Is responsible for converting the given URI parameters into a stringified URI.
   * 
   * @param methodUriPrefix
   *          the prefix of the URI
   * @param methodUriSuffix
   *          the suffix of the URI, not containing the query parameters. A <code>/</code> will split the methodUriPrefix and methodUriSuffix
   *          parameters in the final URI
   * @param uriParameters
   *          a map of key/values that will be used as query parameters in the final URI
   * @return a properly encoded URI
   */
  public String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters);

  /**
   * Is responsible to actually run the relevant HTTP method.
   * 
   * @param uri
   *          the URI against which the HTTP request should be run
   * @param callType
   *          the type of HTTP method
   * @param body
   *          the body of the HTTP method, in case of a {@link WebServiceClient.CallType#Post} or {@link WebServiceClient.CallType#Put} method;
   *          {@code null} otherwise
   * @return the input stream resulting to the HTTP request, which is taken from the response
   * @throws WebServiceClient.CallException
   *           in case an error occurred during the HTTP request execution, or if the HTTP request status code is not {@code 2XX}
   */
  public InputStream getInputStream(String uri, WebServiceClient.CallType callType, HttpEntity body)
      throws WebServiceClient.CallException;

}
