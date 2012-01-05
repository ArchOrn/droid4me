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

package com.smartnsoft.droid4me.app;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * The class that should be used when extending a legacy class to support the whole droid4me framework features.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @param <ComponentClass>
 *          the instance that will be used to determine whether {@linkplain #onRetrieveBusinessObjects() the business object should be retrieved
 *          asynchronously}, and to {@linkplain #registerBroadcastListeners(BroadcastListener[]) register broadcast listeners}
 * 
 * @author Édouard Mercier
 * @since 2011.06.14
 */
public final class Droid4mizer<AggregateClass, ComponentClass>
    implements SmartableActivity<AggregateClass>, Droid4mizerInterface
{

  private static final Logger log = LoggerFactory.getInstance("SmartableActivity");

  private final Activity activity;

  private final ComponentClass component;

  private final ComponentClass interceptorComponent;

  private final SmartableActivity<AggregateClass> smartableActivity;

  private final Droid4mizerInterface droid4mizerInterface;

  private final AppInternals.StateContainer<AggregateClass, ComponentClass> stateContainer;

  /**
   * The only way to create an instance.
   * 
   * @param activity
   *          the activity this instance relies on
   * @param smartableActivity
   *          the component to be droid4mized
   * @param droid4mizerInterface
   *          the extension used for extending the component behavior
   * @param component
   *          the declared component used to determine whether {@linkplain #onRetrieveBusinessObjects() the business object should be retrieved
   *          asynchronously}, and to {@linkplain #registerBroadcastListeners(BroadcastListener[]) register broadcast listeners}
   * @param interceptorComponent
   *          the declared component used to send life-cycle events to the {@link ActivityController.Interceptor}
   */
  public Droid4mizer(Activity activity, SmartableActivity<AggregateClass> smartableActivity, Droid4mizerInterface droid4mizerInterface,
      ComponentClass component, ComponentClass interceptorComponent)
  {
    this.activity = activity;
    this.smartableActivity = smartableActivity;
    this.droid4mizerInterface = droid4mizerInterface;
    this.component = component;
    this.interceptorComponent = interceptorComponent;
    stateContainer = new AppInternals.StateContainer<AggregateClass, ComponentClass>(activity, component);
  }

  public AggregateClass getAggregate()
  {
    return stateContainer.getAggregate();
  }

  public void setAggregate(AggregateClass aggregate)
  {
    stateContainer.setAggregate(aggregate);
  }

  public Handler getHandler()
  {
    return stateContainer.getHandler();
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return smartableActivity.getMenuCommands();
  }

  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    ActivityController.getInstance().handleException(activity, component, throwable);
  }

  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    stateContainer.registerBroadcastListeners(broadcastListeners);
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return stateContainer.isRefreshingBusinessObjectsAndDisplay();
  }

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(true, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(boolean, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects)
  {
    refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, null, false);
  }

  public void refreshBusinessObjectsAndDisplay(final boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
  {
    if (stateContainer.shouldDelayRefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately) == true)
    {
      return;
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStart();
    // We can safely retrieve the business objects
    if (!(component instanceof LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy))
    {
      if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
      {
        return;
      }
      onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
    }
    else
    {
      // We call that routine asynchronously in a background thread
      AppInternals.execute(activity, new Runnable()
      {
        public void run()
        {
          if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
          {
            return;
          }
          // We are handling the UI, and we need to make sure that this is done through the GUI thread
          activity.runOnUiThread(new Runnable()
          {
            public void run()
            {
              onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
            }
          });
        }
      });
    }
  }

  public final int getOnSynchronizeDisplayObjectsCount()
  {
    return stateContainer.getOnSynchronizeDisplayObjectsCount();
  }

  public final boolean isFirstLifeCycle()
  {
    return stateContainer.isFirstLifeCycle();
  }

  public final boolean isInteracting()
  {
    return stateContainer.isInteracting();
  }

  public void onActuallyCreated()
  {
  }

  public void onActuallyDestroyed()
  {
  }

  public boolean shouldKeepOn()
  {
    return stateContainer.shouldKeepOn();
  }

  /*
   * The {@link Activity} methods.
   */

  public void onCreate(Runnable superMethod, Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreate");
    }

    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    superMethod.run();
    if (ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(AppInternals.ALREADY_STARTED) == true)
    {
      stateContainer.setFirstLifeCycle(false);
    }
    else
    {
      stateContainer.setFirstLifeCycle(true);
      onActuallyCreated();
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
    }
    stateContainer.registerBroadcastListeners();

    stateContainer.initialize();
    droid4mizerInterface.onBeforeRetrievingDisplayObjects();
    // ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsBefore);
    try
    {
      onRetrieveDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.stopHandling();
      onException(throwable, true);
      return;
    }
    // ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsAfter);
    // We add the static menu commands
    droid4mizerInterface.getCompositeActionHandler().add(new MenuHandler.Static()
    {
      @Override
      protected List<MenuCommand<Void>> retrieveCommands()
      {
        final List<StaticMenuCommand> staticMenuCommands = getMenuCommands();
        if (staticMenuCommands == null)
        {
          return null;
        }
        final ArrayList<MenuCommand<Void>> menuCommands = new ArrayList<MenuCommand<Void>>(staticMenuCommands.size());
        for (StaticMenuCommand staticMenuCommand : staticMenuCommands)
        {
          menuCommands.add(staticMenuCommand);
        }
        return menuCommands;
      }
    });
  }

  public void onNewIntent(Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onNewIntent");
    }

    if (ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
    }
  }

  public void onContentChanged()
  {
    if (stateContainer.shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onContentChanged);
  }

  public void onResume()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onResume");
    }
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onResume);
    stateContainer.onResume();
    businessObjectRetrievalAndResultHandlers();
  }

  public void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onSaveInstanceState");
    }
    stateContainer.onSaveInstanceState(outState);
    outState.putBoolean(AppInternals.ALREADY_STARTED, true);
  }

  public void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onRestoreInstanceState");
    }
    businessObjectRetrievalAndResultHandlers();
  }

  public void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStart");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart();
  }

  public void onPause()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPause");
    }
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onPause);
      stateContainer.onPause();
    }
  }

  public void onStop()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStop");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStop);
    stateContainer.onStop();
  }

  public void onDestroy()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onDestroy");
    }
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    if (stateContainer.isDoNotCallOnActivityDestroyed() == false)
    {
      onActuallyDestroyed();
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onDestroy);
    }
    stateContainer.unregisterBroadcastListeners();
  }

  public boolean onCreateOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreateOptionsMenu");
    }
    stateContainer.compositeActionHandler.onCreateOptionsMenu(activity, menu);
    return superResult;
  }

  public boolean onPrepareOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPrepareOptionsMenu");
    }
    stateContainer.compositeActionHandler.onPrepareOptionsMenu(menu);
    return superResult;
  }

  public boolean onOptionsItemSelected(boolean superResult, MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onOptionsItemSelected");
    }
    if (stateContainer.compositeActionHandler.onOptionsItemSelected(item) == true)
    {
      return true;
    }
    return superResult;
  }

  public boolean onContextItemSelected(boolean superResult, MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onContextItemSelected");
    }
    if (stateContainer.compositeActionHandler.onContextItemSelected(item) == true)
    {
      return true;
    }
    return superResult;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onActivityResult");
    }

    // // BUG: this seems to be a bug in Android, because this method is invoked before the "onResume()"
    // try
    // {
    // businessObjectRetrievalAndResultHandlers();
    // }
    // catch (Throwable throwable)
    // {
    // handleUnhandledException(throwable);
    // return;
    // }

    droid4mizerInterface.getCompositeActivityResultHandler().handle(requestCode, resultCode, data);
  }

  /*
   * The Droid4mizerInterface interface implementation.
   */

  public Composite getCompositeActionHandler()
  {
    return stateContainer.compositeActionHandler;
  }

  public void onBeforeRetrievingDisplayObjects()
  {
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return stateContainer.compositeActivityResultHandler;
  }

  /*
   * The LifeCycle interface implementation.
   */

  public void onFulfillDisplayObjects()
  {
    smartableActivity.onFulfillDisplayObjects();
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    smartableActivity.onRetrieveBusinessObjects();
  }

  public void onRetrieveDisplayObjects()
  {
    smartableActivity.onRetrieveDisplayObjects();
  }

  public void onSynchronizeDisplayObjects()
  {
    smartableActivity.onSynchronizeDisplayObjects();
  }

  /*
   * The specific methods.
   */

  /**
   * This method should not trigger any exception!
   */
  private boolean onRetrieveBusinessObjectsInternal(boolean retrieveBusinessObjects)
  {
    onBeforeRefreshBusinessObjectsAndDisplay();
    if (retrieveBusinessObjects == true)
    {
      try
      {
        onRetrieveBusinessObjects();
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
        onInternalBusinessObjectAvailableException(throwable);
        return false;
      }
    }
    stateContainer.setBusinessObjectsRetrieved();
    return true;
  }

  private void onBeforeRefreshBusinessObjectsAndDisplay()
  {
    // THINK: should we plug the feature?
  }

  private void onFulfillAndSynchronizeDisplayObjectsInternal(Runnable onOver)
  {
    if (stateContainer.isResumedForTheFirstTime() == true)
    {
      try
      {
        onFulfillDisplayObjects();
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
        onException(throwable, true);
        stateContainer.onStopLoading();
        return;
      }
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent,
          ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
    }
    try
    {
      stateContainer.onSynchronizeDisplayObjects();
      onSynchronizeDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
      onException(throwable, true);
      return;
    }
    finally
    {
      stateContainer.onStopLoading();
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent,
        ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
    stateContainer.markNotResumedForTheFirstTime();
    if (onOver != null)
    {
      onOver.run();
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
  }

  private void businessObjectRetrievalAndResultHandlers()
  {
    smartableActivity.refreshBusinessObjectsAndDisplay(stateContainer.isRetrieveBusinessObjects(), stateContainer.getRetrieveBusinessObjectsOver(), true);
    if (stateContainer.actionResultsRetrieved == false)
    {
      onRegisterResultHandlers(stateContainer.compositeActivityResultHandler);
      stateContainer.actionResultsRetrieved = true;
    }
  }

  private final void onInternalBusinessObjectAvailableException(Throwable throwable)
  {
    if (log.isErrorEnabled())
    {
      log.error("Cannot retrieve the business objects", throwable);
    }
    stateContainer.onStopLoading();
    // We need to invoke that method on the GUI thread, because that method may have been triggered from another thread
    onException(throwable, false);
  }

  private void onRegisterResultHandlers(CompositeHandler compositeActivityResultHandler)
  {
    // THINK: should we plug the feature?
  }

  public SharedPreferences getPreferences()
  {
    return stateContainer.getPreferences(activity);
  }

}
