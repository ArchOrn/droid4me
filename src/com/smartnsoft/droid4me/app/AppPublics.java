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

package com.smartnsoft.droid4me.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.view.View;

import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * Gathers some interfaces and helpers for the types belonging to its Java package.
 * 
 * @author Édouard Mercier
 * @since 2010.01.05
 */
public final class AppPublics
{

  /**
   * Defines common methods for all {@link Activity activity classes} defined in the framework.
   * 
   * @since 2010.07.10
   */
  public interface CommonActivity
  {

    /**
     * @return a valid handler that may be used for processing GUI-thread operations.
     */
    Handler getHandler();

    /**
     * @return an object that may be used along the {@link Activity} life ; may return <code>null</code>
     * @see #setAggregate(Object)
     */
    Object getAggregate();

    /**
     * Enables to set an aggregate hat may be used along the {@link Activity} life.
     * 
     * @param aggregate
     *          the object to use as an aggregate
     * @see #getAggregate()
     */
    void setAggregate(Object aggregate);

    /**
     * @return all the static menu commands that the activity wants to make available ; may return <code>null</code>
     */
    List<StaticMenuCommand> getMenuCommands();

    /**
     * Can be invoked when an exception is thrown, so that the {@link ActivityController.ExceptionHandler} handles it.
     * 
     * @param fromGuiThread
     *          indicates whether the call is done from the GUI thread
     */
    void onException(Throwable throwable, boolean fromGuiThread);

  }

  /**
   * Because an Android {@link Activity} can be destroyed and then recreated, following a screen orientation change, for instance, this interface
   * gives information about an entity life cycle.
   * 
   * @since 2010.01.05
   */
  public interface LifeCyclePublic
  {

    /**
     * Is not invoked by the framework, and provides information about the current entity life cycle.
     * 
     * @return <code>true</code> if and only if the entity life cycle is the first time to execute during its container life
     */
    boolean isFirstLifeCycle();

  }

  /**
   * Use this intent action when you need to indicate that something is being loaded. For indicating that the loading is over, attach the
   * {@link AppPublics#UI_LOAD_ACTION_LOADING} key to the {@link Intent}.
   */
  public static String UI_LOAD_ACTION = "com.smartnsoft.droid4me.action.UI_LOADING";

  /**
   * A flag name which indicates the FQN name of the activity class which is loading.
   */
  public final static String UI_LOAD_ACTION_ACTIVITY = "activity";

  /**
   * A flag name which indicates whether the loading event is starting or ending.
   */
  public final static String UI_LOAD_ACTION_LOADING = "loading";

  /**
   * Use this intent action when you need to indicate that an activity should refresh its UI.
   */
  public static String UPDATE_ACTION = "com.smartnsoft.droid4me.action.UPDATE";

  /**
   * Use this intent action when you need to indicate that an activity should reload its business objects.
   */
  public static String RELOAD_ACTION = "com.smartnsoft.droid4me.action.RELOAD";

  /**
   * Defined in order to compute the default intent actions.
   */
  static void initialize(Application application)
  {
    AppPublics.UI_LOAD_ACTION = application.getPackageName() + ".action.UI_LOADING";
    AppPublics.UPDATE_ACTION = application.getPackageName() + ".action.UPDATE";
    AppPublics.RELOAD_ACTION = application.getPackageName() + ".action.RELOAD";
    AppPublics.MultiSelectionHandler.ACTION_SELECTION = application.getPackageName() + ".action.SELECTION";
  }

  /**
   * Indicates what kind of {@link Intent} are being listened to, and how to handle an intent.
   * 
   * @since 2010.02.04
   */
  public interface BroadcastListener
  {

    /**
     * @return if not <code>null</code>, only the intents that match with this returned value, will be received by the activity
     */
    IntentFilter getIntentFilter();

    /**
     * Is invoked every time an intent that matches is received by the underlying activity.
     */
    void onReceive(Intent intent);

  }

  /**
   * States that the Android {@link Activity} which implements this interface is able to provide an intent broadcast listener.
   * 
   * @see AppPublics#BroadcastListenersProvider
   * @since 2010.02.04
   */
  public interface BroadcastListenerProvider
  {

    /**
     * @return is not allowed to be null!
     */
    AppPublics.BroadcastListener getBroadcastListener();

  }

  /**
   * States that the Android {@link Activity} which implements this interface is able to provide several broadcast listener.
   * 
   * @see AppPublics#BroadcastListenerProvider
   * @since 2010.11.07
   */
  public interface BroadcastListenersProvider
  {

    /**
     * @return the number of {@link AppPublics.BroadcastListener} which are supported
     */
    int getBroadcastListenersCount();

    /**
     * @param the
     *          index of the {@link AppPublics.BroadcastListener} to return
     * @return is not allowed to be null!
     */
    AppPublics.BroadcastListener getBroadcastListener(int index);

  }

  /**
   * When an {@link Activity} implements that interface, it will send broadcast intents while loading and once the loading is over.
   * 
   * @since 2010.02.04
   * @see AppPublics.BroadcastListener
   * @see AppPublics.BroadcastListenerProvider
   */
  public interface SendLoadingIntent
  {
  }

  /**
   * A broadcast listener which listens only to {@link AppPublics#UI_LOAD_ACTION} intents.
   * 
   * @since 2010.02.04
   */
  public static abstract class LoadingBroadcastListener
      implements AppPublics.BroadcastListener
  {

    private final Activity activity;

    private int counter = 0;

    private boolean restrictToActivity;

    public LoadingBroadcastListener(Activity activity)
    {
      this(activity, true);
    }

    /**
     * @param restrictToActivity
     *          indicates whether the listener should restrict to the {@link Intent} sent by the provided {@link Activity}
     */
    public LoadingBroadcastListener(Activity activity, boolean restrictToActivity)
    {
      this.activity = activity;
      this.restrictToActivity = restrictToActivity;
    }

    public IntentFilter getIntentFilter()
    {
      final IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(AppPublics.UI_LOAD_ACTION);
      if (restrictToActivity == true)
      {
        intentFilter.addCategory(activity.getClass().getName());
      }
      return intentFilter;
    }

    public void onReceive(Intent intent)
    {
      if (intent.hasExtra(AppPublics.UI_LOAD_ACTION_ACTIVITY) == true && intent.getStringExtra(AppPublics.UI_LOAD_ACTION_ACTIVITY).equals(
          activity.getClass().getName()) == true)
      {
        final int previousCounter = counter;
        // We only take into account the loading event coming from the activity itself
        counter += (intent.getBooleanExtra(AppPublics.UI_LOAD_ACTION_LOADING, true) == true ? 1 : -1);

        // We only trigger an event provided the cumulative loading status has changed
        if (previousCounter == 0 && counter >= 1)
        {
          onLoading(true);
        }
        else if (previousCounter >= 1 && counter <= 0)
        {
          onLoading(false);
        }
        // activity.setProgressBarIndeterminateVisibility(counter > 0);
      }
    }

    /**
     * Is invoked when a new loading event (intent) occurs.
     * 
     * @param isLoading
     *          is equal to <code>true</code> if and only if the underlying activity is not loading anymore
     */
    protected abstract void onLoading(boolean isLoading);

  }

  /**
   * A helper which receives mutli-selection {@link Intent intents} with an action set to {@link MultiSelectionHandler#ACTION_SELECTION}, and which
   * remembers and triggers event according to the current selection.
   * 
   * <p>
   * The <code>BusinessObjectClass</code> "template" class must implement properly the {@link Object#equals(Object)} method, in order to ensure the
   * unicity of business objects inside the {@link MultiSelectionHandler#getSelectedObjects() selected business objects}.
   * </p>
   * 
   * @since 2010.06.21
   */
  public final static class MultiSelectionHandler<BusinessObjectClass extends Serializable>
  {

    /**
     * The interface which is invoked by the {@link MultiSelectionHandler} when a new selection event occurs.
     */
    public static interface OnMultiSelectionChanged<BusinessObjectClass>
    {

      /**
       * Invoked when the selection changes.
       * 
       * @param previouslyAtLeastOneSelected
       *          the number of items selected before the event occurred
       * @param atLeastOneSelected
       *          the new number of items selected
       * @param selectedObjects
       *          the business objects that are currently selected
       */
      void onSelectionChanged(boolean previouslyAtLeastOneSelected, boolean atLeastOneSelected, List<BusinessObjectClass> selectedObjects);
    }

    /**
     * Use this intent action when you need to indicate to the current activity that a business object has been selected or unselected.
     * 
     * @see use the {@link MultiSelectionHandler#SELECTED} and {@link MultiSelectionHandler#BUSINESS_OBJECT} extra flags for indicated whether the
     *      business object is selected, and what business object this is about
     */
    public static String ACTION_SELECTION = "com.smartnsoft.droid4me.action.SELECTION";

    public final static String SELECTED = "selected";

    public final static String BUSINESS_OBJECT = "businessObject";

    private int selectedCount = 0;

    /**
     * Keeps in memory all the selected objects list.
     */
    private final List<BusinessObjectClass> selectedObjects = new ArrayList<BusinessObjectClass>();

    /**
     * @return the number of business objects currently selected
     */
    public final int getSelectedCount()
    {
      return selectedCount;
    }

    /**
     * @return the currently selected business objects. Do not modify the returned list!
     */
    public final List<BusinessObjectClass> getSelectedObjects()
    {
      return selectedObjects;
    }

    /**
     * Clears the selected objects.
     */
    public final void clearSelection()
    {
      selectedObjects.clear();
      selectedCount = 0;
    }

    /**
     * The method to invoke when the activity receives a {@link MultiSelectionHandler#ACTION_SELECTION intent}, due to as business object
     * selection/unselection.
     * 
     * @param intent
     *          the received intent; if the action of the Intent is not the right one, no processing is done
     * @param onMultiSelectionChanged
     *          the interface that will be callbacked depending on the overall multi-selection state
     * @return <code>true</code> if the intent has been handled ; <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean onSelection(Intent intent, MultiSelectionHandler.OnMultiSelectionChanged onMultiSelectionChanged)
    {
      if (intent.getAction().equals(AppPublics.MultiSelectionHandler.ACTION_SELECTION) == false)
      {
        return false;
      }
      final boolean selected = intent.getBooleanExtra(MultiSelectionHandler.SELECTED, false) == true;
      final int previousSelectedCount = selectedCount;
      final BusinessObjectClass businessObject = (BusinessObjectClass) intent.getSerializableExtra(MultiSelectionHandler.BUSINESS_OBJECT);
      setSelection(businessObject, selected);
      onMultiSelectionChanged.onSelectionChanged(previousSelectedCount >= 1, selectedCount >= 1, selectedObjects);
      return true;
    }

    /**
     * Sets the selection state for a given business object.
     * 
     * @param businessObject
     *          the business object related to that selection event
     * @param selected
     *          whether the business object should be considered as selected
     */
    public void setSelection(BusinessObjectClass businessObject, boolean selected)
    {
      selectedCount += (selected == true ? 1 : -1);
      if (businessObject != null)
      {
        if (selected == true)
        {
          if (selectedObjects.contains(businessObject) == false)
          {
            selectedObjects.add(businessObject);
          }
        }
        else
        {
          selectedObjects.remove(businessObject);
        }
      }
    }

  }

  /**
   * A {@link Runnable} which is allowed to throw an exception during its execution.
   * 
   * <p>
   * Defined for being able to run {@link Runnable} which throw exceptions within the {@link SmartThreadPoolExecutor}.
   * </p>
   * 
   * @since 2010.06.08
   */
  public abstract static class GuardedCommand
      implements Runnable
  {

    private final Context context;

    /**
     * @param context
     *          the context from which the execution originates, and which will be used when reporting a potential exception ; it is not allowed to be
     *          <code>null</code>
     */
    public GuardedCommand(Context context)
    {
      if (context == null)
      {
        throw new NullPointerException("The context should not be null!");
      }
      this.context = context;
    }

    /**
     * @return the activity which will be used for reporting a potential exception
     */
    protected final Context getContext()
    {
      return context;
    }

    /**
     * The body of the command execution.
     * 
     * @throws Exception
     *           the method allows an exception to be thrown, and will be appropriately caught by the
     *           {@link ActivityController#handleException(Activity, Throwable)} method
     */
    protected abstract void runGuarded()
        throws Exception;

    public final void run()
    {
      try
      {
        runGuarded();
      }
      catch (Throwable throwable)
      {
        // We handle the exception
        ActivityController.getInstance().handleException(context, throwable);
      }
    }

  }

  /**
   * Enables to execute in background a task, by notifying when the execution is running. It is especially useful when the UI should be notified when
   * a command is running.
   * 
   * <p>
   * When the command is executed by the {@link AppPublics#LOW_PRIORITY_THREAD_POOL}, its underlying {@link ProgressHandler} is notified.
   * </p>
   * 
   * @since 2010.11.30
   */
  public static abstract class ProgressGuardedCommand
      extends AppPublics.GuardedCommand
  {

    private final ProgressHandler progressHandler;

    private final String message;

    /**
     * @param activity
     *          the activity that initiates the command
     * @param progressHandler
     *          it will be invoked when the command {@link #runGuardedProgress() starts}, and when the command is over, evn if an exception is trhown
     *          during the execution
     * @param message
     *          an optional message that will be passed to the {@link ProgressHandler} when the command starts
     */
    public ProgressGuardedCommand(Activity activity, ProgressHandler progressHandler, String message)
    {
      super(activity);
      this.progressHandler = progressHandler;
      this.message = message;
    }

    /**
     * The commands to execute during the task.
     * 
     * @throws Exception
     *           any thrown exception will be properly handled
     */
    protected abstract void runGuardedProgress()
        throws Exception;

    @Override
    protected final void runGuarded()
        throws Exception
    {
      try
      {
        progressHandler.onProgress((Activity) getContext(), true, new ProgressHandler.ProgressExtra(0, message), false);
        runGuardedProgress();
      }
      finally
      {
        progressHandler.onProgress((Activity) getContext(), false, null, false);
      }
    }

  }

  /**
   * Introduced so as to be able to catch the exceptions thrown in the framework thread pools.
   * 
   * @since 2010.03.02
   */
  public final static class SmartThreadPoolExecutor
      extends ThreadPoolExecutor
  {

    private SmartThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory)
    {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * The same method as {@link #execute(Runnable)} except that it transfers the activity to the {@link ActivityController.ExceptionHandler} when an
     * exception occurs.
     * 
     * @param activity
     *          the activity which is responsible for running the piece of code
     * @param command
     *          the piece of code to execute
     */
    public void execute(Activity activity, Runnable command)
    {
      try
      {
        super.execute(command);
      }
      catch (Throwable throwable)
      {
        // We handle the exception
        ActivityController.getInstance().handleException(activity, throwable);
      }
    }

    /**
     * Executes the given command. Should be used when the {@link Runnable} may throw an {@link Exception}.
     * 
     * @param guardedCommand
     *          the command to run
     */
    public void execute(AppPublics.GuardedCommand guardedCommand)
    {
      super.execute(guardedCommand);
    }

    /**
     * Just calls its @{link #execute(Activity, Runnable)} counterpart, with a null activity.
     * 
     * @deprecated should not be used!
     */
    @Override
    public void execute(Runnable command)
    {
      this.execute(null, command);
    }

  }

  private static int normalPriorityThreadCount = 1;

  private static int lowPriorityThreadCount = 1;

  /**
   * This threads pool is used internally, in order to prevent from new thread creation, for an optimization purpose.
   * 
   * <ul>
   * <li>This pool can contain an unlimited number of threads;</li>
   * <li>exceptions thrown by the {@link Runnable} are handled by the {@link ActivityController.ExceptionHandler}.</li>
   * </ul>
   * 
   * <p>
   * You can use this pool in the application, instead of creating new threads.
   * </p>
   * 
   * @see AppPublics#LOW_PRIORITY_THREAD_POOL
   */
  public final static AppPublics.SmartThreadPoolExecutor THREAD_POOL = new AppPublics.SmartThreadPoolExecutor(0, Integer.MAX_VALUE, 60l, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-pool-thread #" + AppPublics.normalPriorityThreadCount++);
      return thread;
    }
  });

  /**
   * Indicates how many threads at most will be available in the {@link #LOW_PRIORITY_THREAD_POOL low-priority threads pool}, by default. It needs to
   * be sent at the application start-up.
   */
  public final static int LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE = 3;

  /**
   * Use this threads pool instead of creating your own {@link Thread#MIN_PRIORITY} threads.
   * 
   * <ul>
   * <li>This pool will contain at most {@link #LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE} threads by default;</li>
   * <li>exceptions thrown by the {@link Runnable} are handled by the {@link ActivityController.ExceptionHandler}.</li>
   * </ul>
   * 
   * <p>
   * You can use this pool in the application, instead of creating new threads.
   * </p>
   * 
   * @see AppPublics#THREAD_POOL
   */
  public final static AppPublics.SmartThreadPoolExecutor LOW_PRIORITY_THREAD_POOL = new AppPublics.SmartThreadPoolExecutor(AppPublics.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, AppPublics.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, 10l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.setName("droid4me-lowpool-thread #" + AppPublics.lowPriorityThreadCount++);
      return thread;
    }
  });

  /**
   * A {@link DialogInterface.OnClickListener} which runs its {@link AppPublics.GuardedCommand#runGuarded() execution} in the
   * {@link AppPublics#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   * 
   * @since 2010.06.08
   */
  public static abstract class GuardedDialogClickListener
      extends AppPublics.GuardedCommand
      implements DialogInterface.OnClickListener
  {

    /**
     * @param activity
     *          the activity from which the execution originates, and which will be used when reporting a potential exception
     */
    public GuardedDialogClickListener(Activity activity)
    {
      super(activity);
    }

    public final void onClick(DialogInterface dialog, int which)
    {
      AppPublics.LOW_PRIORITY_THREAD_POOL.execute(this);
    }

  }

  /**
   * A {@link View.OnClickListener} which runs its {@link AppPublics.GuardedCommand#runGuarded() execution} in the
   * {@link AppPublics#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   * 
   * @since 2010.06.08
   */
  public static abstract class GuardedViewClickListener
      extends AppPublics.GuardedCommand
      implements View.OnClickListener
  {

    /**
     * @param activity
     *          the activity from which the execution originates, and which will be used when reporting a potential exception
     */
    public GuardedViewClickListener(Activity activity)
    {
      super(activity);
    }

    public final void onClick(View view)
    {
      AppPublics.LOW_PRIORITY_THREAD_POOL.execute(this);
    }

  }

}