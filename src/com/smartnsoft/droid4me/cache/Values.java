/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.IOStreamer;
import com.smartnsoft.droid4me.bo.Business.UriStreamParser;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Gathers interfaces and classes for handling business objects in cache.
 * 
 * @author Édouard Mercier
 * @since 2009.08.30
 */
public final class Values
{

  private final static Logger log = LoggerFactory.getInstance(Values.class);

  /**
   * Gathers a business object and its time stamp.
   */
  public static class Info<BusinessObjectType>
  {

    public final static int SOURCE1 = 0;

    public final static int SOURCE2 = SOURCE1 + 1;

    public final static int SOURCE3 = SOURCE2 + 1;

    public final BusinessObjectType value;

    public final Date timestamp;

    public final int source;

    public Info(BusinessObjectType value, Date timestamp, int source)
    {
      this.value = value;
      this.timestamp = timestamp;
      this.source = source;
    }

  }

  /**
   * Used when requesting a {@link Values.CacheableValue} or a {@link CachedMap} for retrieving a business object, depending on the current workflow.
   * 
   * @since 2010.09.09
   */
  public static interface CachingEvent
  {

    /**
     * Is invoked when the wrapped business object is bound to be retrieved from an {@link IOStreamer}, i.e. from the persistence layer.
     */
    void onFromIOStreamer();

    /**
     * Is invoked when the wrapped business object is bound to be retrieved from an {@link UriStreamParser}, i.e. not locally.
     */
    void onFromUriStreamParser();

  }

  /**
   * Used and required when the business object is not available in memory.
   */
  public static interface Instructions<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    /**
     * Indicates whether the business object should be first taken from the cache.
     * 
     * @return <code>true</code> if and only if the business object should be tested from the cache
     * @see #accept(Values.Info)
     */
    boolean tryLoadedFirst();

    /**
     * Is invoked every time the business object is taken from the cache, in order to decide whether it is valid.
     * 
     * @param info
     *          the proposed business object and its associated timestamp
     * @return <code>true</code> if and only if the proposed business is accepted
     */
    boolean accept(Values.Info<BusinessObjectType> info);

    /**
     * Is invoked when the underlying business object will not be taken from the memory cache, or when the cached value has been rejected.
     * 
     * @param previouslyRejected
     *          indicates whether the retrieval of the business object follows a previous retrieval, which was not {@link #accept(Values.Info)
     *          accepted}
     * @param cachingEvent
     *          the interface that will be used to notify the caller about the loading workflow ; may be <code>null</code>
     * @return the wrapped business object
     */
    Values.Info<BusinessObjectType> onNotFromLoaded(boolean previouslyRejected, Values.CachingEvent cachingEvent)
        throws ExceptionType, ProblemExceptionType;

    /**
     * Will be invoked when the business object cannot be eventually retrieved.
     * 
     * @param causeException
     *          the reason which states why the business object cannot be retrieved
     * @return an exception to be thrown to the caller
     */
    ProblemExceptionType onUnaccessible(Exception causeException);

  }

  public final static class InstructionsException
      extends Exception
  {

    private static final long serialVersionUID = 1724469505270150039L;

    private InstructionsException(String message)
    {
      super(message);
    }

  }

  /**
   * Defines a basic contract which enables to cache a business object in memory, know when it has been cached, and have a dynamic strategy for
   * refreshing the object.
   * 
   * @since 2009.06.18
   */
  public static interface CacheableValue<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    boolean isEmpty();

    BusinessObjectType getLoadedValue();

    void setLoadedInfoValue(Values.Info<BusinessObjectType> info);

    BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ProblemExceptionType> instructions, Values.CachingEvent cachingEvent)
        throws ExceptionType, ProblemExceptionType;

  }

  /**
   * Defines a common class for all "cacheables", i.e. {@link CacheableValue} and {@link BackedCachedMap}, so that we can register them.
   * 
   * @since 2010.06.25
   */
  public static abstract class Caching
  {

    private final static List<Values.Caching> instances = new ArrayList<Values.Caching>();

    protected Caching()
    {
      Caching.instances.add(this);
    }

    /**
     * Is supposed to empty the underlying cached value(s). Invoking {@link #isEmpty()} afterwards will return <code>true</code.
     */
    public abstract void empty();

    /**
     * Empties all {@link Values.Caching} instances.
     */
    public static synchronized void emptyAll()
    {
      for (Values.Caching caching : instances)
      {
        caching.empty();
      }
    }

  }

  /**
   * Enables to cache a business object in memory only.
   * 
   * @since 2009.06.18
   */
  public static class CachedValue<BusinessObjectType, ExceptionType extends Exception>
      extends Values.Caching
      implements Values.CacheableValue<BusinessObjectType, ExceptionType, ExceptionType>
  {

    private Values.Info<BusinessObjectType> info;

    public final boolean isEmpty()
    {
      return info == null;
    }

    public final Values.Info<BusinessObjectType> getLoadedInfoValue()
    {
      return info;
    }

    public final BusinessObjectType getLoadedValue()
    {
      if (info == null)
      {
        return null;
      }
      return info.value;
    }

    public final void setLoadedInfoValue(Values.Info<BusinessObjectType> info)
    {
      this.info = info;
    }

    public final Values.Info<BusinessObjectType> getInfoValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> instructions,
        Values.CachingEvent cachingEvent)
        throws ExceptionType
    {
      if (instructions.tryLoadedFirst() == false)
      {
        Exception onNotFromLoadedException;
        try
        {
          final Values.Info<BusinessObjectType> newInfo = instructions.onNotFromLoaded(false, cachingEvent);
          if (newInfo != null)
          {
            setLoadedInfoValue(newInfo);
            return newInfo;
          }
          else
          {
            throw instructions.onUnaccessible(new Values.InstructionsException("Cannot access to the live data when the data should not be taken from the cache!"));
          }
        }
        // The 'ExceptionType' exception
        catch (Exception exception)
        {
          // We try to retrieve the object via the cache
          onNotFromLoadedException = exception;
        }
        if (isEmpty() == false && instructions.accept(info) == true)
        {
          return getLoadedInfoValue();
        }
        throw instructions.onUnaccessible(onNotFromLoadedException);
      }
      else
      {
        // The business object is first attempted to be retrieved from memory
        if (isEmpty() == false && instructions.accept(info) == true)
        {
          return getLoadedInfoValue();
        }
        final Values.Info<BusinessObjectType> newInfo = instructions.onNotFromLoaded(false, cachingEvent);
        if (newInfo != null)
        {
          // We check whether the newly retrieved info is accepted
          if (instructions.accept(newInfo) == true)
          {
            setLoadedInfoValue(newInfo);
            return newInfo;
          }
          else
          {
            final Values.Info<BusinessObjectType> reloadedInfo = instructions.onNotFromLoaded(true, cachingEvent);
            if (reloadedInfo != null)
            {
              setLoadedInfoValue(reloadedInfo);
              return reloadedInfo;
            }
          }
        }
        throw instructions.onUnaccessible(new Values.InstructionsException("Cannot access to the live business object when the data should not be taken from the cache!"));
      }
    }

    public final BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> instructions,
        Values.CachingEvent cachingEvent)
        throws ExceptionType
    {
      final Info<BusinessObjectType> infoValue = getInfoValue(instructions, cachingEvent);
      return infoValue == null ? null : infoValue.value;
    }

    @Override
    public final void empty()
    {
      info = null;
    }

  }

  public final static class CacheException
      extends Business.BusinessException
  {

    private static final long serialVersionUID = -2742319642305884562L;

    private CacheException(String message, Throwable cause)
    {
      super(message, cause);
    }

    private CacheException(Throwable cause)
    {
      super(cause);
    }

  }

  public abstract static class WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      implements Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException>
  {

    protected final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    protected final ParameterType parameter;

    protected WithParameterInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher, ParameterType parameter)
    {
      this.cacher = cacher;
      this.parameter = parameter;
    }

    public final Values.CacheException onUnaccessible(Exception exception)
    {
      return new Values.CacheException("Could not access to the business object neither through the cache nor through the IO streamer", exception);
    }

  };

  public static class OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public OnlyFromCacheInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory)
    {
      super(cacher, parameter);
      this.fromMemory = fromMemory;
    }

    public boolean tryLoadedFirst()
    {
      return fromMemory == true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return true;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(boolean previouslyRejected, Values.CachingEvent cachingEvent)
        throws Values.CacheException
    {
      try
      {
        return cacher.getCachedValue(parameter);
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not read from the cache the business object corresponding to the URI '" + parameter + "'", throwable);
      }
    }

  };

  public static class MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromCache;

    public MemoryInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache)
    {
      super(cacher, parameter);
      this.fromCache = fromCache;
    }

    public boolean tryLoadedFirst()
    {
      return true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return isTakenFromCache(fromCache, info.timestamp);
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(final boolean previouslyRejected, final Values.CachingEvent cachingEvent)
        throws Values.CacheException
    {
      try
      {
        return cacher.getValue(new Cacher.Instructions()
        {
          public boolean takeFromCache(Date lastUpdate)
          {
            return previouslyRejected == false ? isTakenFromCache(fromCache, lastUpdate) == true : lastUpdate == null;
          }

          public void onFetchingFromIOStreamer()
          {
            if (cachingEvent != null)
            {
              cachingEvent.onFromIOStreamer();
            }
          }

          public void onFetchingFromUriStreamParser()
          {
            if (cachingEvent != null)
            {
              cachingEvent.onFromUriStreamParser();
            }
          }
        }, parameter);
      }
      catch (Throwable throwable)
      {
        // TODO: add a flag which controls whether a last attempts should be run
        // A last attempt is done to retrieve the data from the cache
        try
        {
          return cacher.getValue(new Cacher.Instructions()
          {
            public boolean takeFromCache(Date lastUpdate)
            {
              return lastUpdate == null ? false : true;
            }

            public void onFetchingFromIOStreamer()
            {
              if (cachingEvent != null)
              {
                cachingEvent.onFromIOStreamer();
              }
            }

            public void onFetchingFromUriStreamParser()
            {
              if (cachingEvent != null)
              {
                cachingEvent.onFromUriStreamParser();
              }
            }
          }, parameter);
        }
        catch (Throwable innerThrowable)
        {
          throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", innerThrowable);
        }
      }
    }

    protected boolean isTakenFromCache(boolean fromCache, Date lastUpdate)
    {
      return lastUpdate == null ? false : fromCache == true;
    }

  };

  public static class MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public MemoryAndCacheInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory, boolean fromCache)
    {
      super(cacher, parameter, fromCache);
      this.fromMemory = fromMemory;
    }

    public boolean tryLoadedFirst()
    {
      return fromMemory == true;
    }

    protected boolean isTakenFromCache(boolean fromCache, Date lastUpdate)
    {
      return lastUpdate == null ? false : fromCache == true;
    }

  };

  public static class RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final long cachingPeriodInMilliseconds;

    public RetentionInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache, long cachingPeriodInMilliseconds)
    {
      super(cacher, parameter, fromCache);
      this.cachingPeriodInMilliseconds = cachingPeriodInMilliseconds;
    }

    protected boolean isTakenFromCache(boolean fromCache, Date lastUpdate)
    {
      return lastUpdate == null ? true
          : fromCache == true && (cachingPeriodInMilliseconds == -1 || ((System.currentTimeMillis() - lastUpdate.getTime()) < cachingPeriodInMilliseconds));
    }

  };

  public static class SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    // private boolean alreadyLoadedFromUriStreamParser;

    public SessionInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter)
    {
      super(cacher, parameter);
    }

    public boolean tryLoadedFirst()
    {
      return true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return info.source == Values.Info.SOURCE3;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(boolean previouslyRejected, Values.CachingEvent cachingEvent)
        throws Values.CacheException
    {
      try
      {
        final Cacher.CachedInfo<BusinessObjectType> info = cacher.getValue(parameter);
        // if (info != null && info.origin == Cacher.Origin.UriStreamParser)
        // {
        // alreadyLoadedFromUriStreamParser = true;
        // }
        return info;
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", throwable);
      }
    }

  };

  /**
   * @since 2009.08.31
   */
  public static class BackedCachedValue<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Values.CachedValue<BusinessObjectType, Values.CacheException>
  {

    public final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedValue(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    public void setValue(ParameterType parameter, BusinessObjectType businessObject)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<BusinessObjectType>(businessObject, new Date(), Values.Info.SOURCE1);
      // Even if cacher fails to persist the business object, the memory is up-to-date
      super.setLoadedInfoValue(info);
      try
      {
        cacher.setValue(parameter, info);
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not save the business object corresponding to the parameter '" + parameter + "'", throwable);
      }
    }

    public final BusinessObjectType safeGet(ParameterType parameter)
    {
      return safeGet(true, true, null, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, ParameterType parameter)
    {
      return safeGet(fromCache, true, null, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
    {
      try
      {
        return getMemoryValue(fromMemory, fromCache, cachingEvent, parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed reading of the business object with parameter '" + parameter + "': returning the cached value if any", exception);
        }
        return getLoadedValue();
      }
    }

    public final void safeSet(ParameterType parameter, BusinessObjectType businessObject)
    {
      try
      {
        setValue(parameter, businessObject);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed saving of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final void remove(ParameterType parameter)
        throws Values.CacheException
    {
      try
      {
        cacher.remove(parameter);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not remove from the persistent cache te business object with parameter '" + parameter + "'", throwable);
          throw new Values.CacheException(throwable);
        }
      }
      setLoadedInfoValue(null);
    }

    public final void safeRemove(ParameterType parameter)
    {
      try
      {
        remove(parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed removing of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final Values.Info<BusinessObjectType> getOnlyFromCacheInfoValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory),
          null);
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getOnlyFromCacheInfoValue(fromMemory, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getSessionInfoValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter),
          cachingEvent);
    }

    public final BusinessObjectType getSessionValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getSessionInfoValue(cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache),
          cachingEvent);
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache),
          cachingEvent);
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromMemory, fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getRetentionInfoValue(boolean fromCache, long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache, cachingPeriodInMilliseconds),
          cachingEvent);
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getRetentionInfoValue(fromCache, cachingPeriodInMilliseconds, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

  }

  /**
   * Enables to cache in memory a map of business objects.
   * 
   * @since 2009.06.18
   */

  public static class CachedMap<BusinessObjectType, KeyType, ExceptionType extends Exception>
      extends Values.Caching
  {

    protected final Map<KeyType, Values.CachedValue<BusinessObjectType, ExceptionType>> map = new ConcurrentHashMap<KeyType, Values.CachedValue<BusinessObjectType, ExceptionType>>();

    public Values.Info<BusinessObjectType> getInfoValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> ifValueNotCached,
        Values.CachingEvent cachingEvent, KeyType key)
        throws ExceptionType
    {
      Values.CachedValue<BusinessObjectType, ExceptionType> cached;
      cached = map.get(key);
      if (map.containsKey(key) == false)
      {
        cached = new Values.CachedValue<BusinessObjectType, ExceptionType>();
        map.put(key, cached);
      }
      return cached.getInfoValue(ifValueNotCached, cachingEvent);
    }

    public BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> ifValueNotCached,
        Values.CachingEvent cachingEvent, KeyType key)
        throws ExceptionType
    {
      final Values.Info<BusinessObjectType> infoValue = getInfoValue(ifValueNotCached, cachingEvent, key);
      return infoValue == null ? null : infoValue.value;
    }

    /**
     * This implementation does not empty the {@link Values.CacheableValue} values.
     * 
     *@see Values.Caching#empty()
     */
    @Override
    public final void empty()
    {
      map.clear();
    }

  }

  /**
   * Enables to cache in memory and on persistent cache a map of business objects.
   * 
   * @since 2009.09.09
   */
  public static final class BackedCachedMap<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Values.CachedMap<BusinessObjectType, ParameterType, Values.CacheException>

  {

    public final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedMap(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    /**
     * If two setting and getting methods are called concurrently, the consistency of the results are not granted!
     */
    @Override
    public final BusinessObjectType getValue(Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException> instructions,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final boolean toAdd;
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        toAdd = true;
        cachedValue = new Values.CachedValue<BusinessObjectType, Values.CacheException>();
      }
      else
      {
        toAdd = false;
      }
      final BusinessObjectType businessObject = cachedValue.getValue(instructions, cachingEvent);
      if (businessObject != null && toAdd == true)
      {
        map.put(parameter, cachedValue);
      }
      return businessObject;
    }

    public final BusinessObjectType getValue(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(true, fromCache, cachingEvent, parameter);
    }

    public final BusinessObjectType getValue(boolean fromMemory, final boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache),
          cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(Values.CachingEvent cachingEvent, ParameterType parameter)
    {
      return safeGet(true, true, cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
    {
      return safeGet(true, fromCache, cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
    {
      try
      {
        return getValue(fromMemory, fromCache, cachingEvent, parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed reading of the business object with parameter '" + parameter + "': returning the cached value if any", exception);
        }
        return getLoadedValue(parameter);
      }
    }

    public final BusinessObjectType getLoadedValue(ParameterType parameter)
    {
      final Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue;
      cachedValue = map.get(parameter);
      if (cachedValue != null)
      {
        return cachedValue.getLoadedValue();
      }
      return null;
    }

    public final void remove(ParameterType parameter)
        throws Values.CacheException
    {
      try
      {
        cacher.remove(parameter);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not remove from the persistent cache te business object with parameter '" + parameter + "'", throwable);
          throw new Values.CacheException(throwable);
        }
      }
      // We clean-up the memory cache, only provided the IO streamer has been cleaned-up
      map.remove(parameter);
    }

    public final void safeRemove(ParameterType parameter)
    {
      try
      {
        remove(parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed removing of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final void safeSet(ParameterType parameter, BusinessObjectType businessObject)
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<BusinessObjectType>(businessObject, new Date(), Values.Info.SOURCE1);
      // We modify the memory first, so as to make sure that it is actually modified
      setLoadedValue(parameter, info);
      try
      {
        cacher.setValue(parameter, info);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Failed to save to the cache the business object with parameter '" + parameter + "': only taken into account in memory!", throwable);
        }
      }
    }

    private void setLoadedValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
    {
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        cachedValue = new Values.CachedValue<BusinessObjectType, Values.CacheException>();
        map.put(parameter, cachedValue);
      }
      cachedValue.setLoadedInfoValue(info);
    }

    public final Values.Info<BusinessObjectType> getOnlyFromCacheInfoValue(boolean fromMemory, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory),
          cachingEvent, parameter);
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getOnlyFromCacheInfoValue(fromMemory, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getSessionInfoValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter),
          cachingEvent, parameter);
    }

    public final BusinessObjectType getSessionValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getSessionInfoValue(cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache),
          cachingEvent, parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache),
          cachingEvent, parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromMemory, fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getRetentionInfoValue(boolean fromCache, long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(
          new Values.RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache, cachingPeriodInMilliseconds),
          cachingEvent, parameter);
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getRetentionInfoValue(fromCache, cachingPeriodInMilliseconds, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

  }

}