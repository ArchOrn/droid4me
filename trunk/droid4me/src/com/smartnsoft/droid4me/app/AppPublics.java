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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.smartnsoft.droid4me.LifeCycle;

/**
 * Gathers some interfaces and helpers for the types belonging to its Java package.
 * 
 * @author �douard Mercier
 * @since 2010.01.05
 */
public final class AppPublics
{

  /**
   * A flag name which indicates the FQN name of the activity class concerned with the action.
   */
  public final static String EXTRA_ACTION_ACTIVITY = "activity";

  /**
   * A flag name which indicates the FQN name of the component class concerned with the action.
   */
  public final static String EXTRA_ACTION_COMPONENT = "component";

  /**
   * Use this intent action when you need to indicate that something is being loaded. For indicating that the loading is over, attach the
   * {@link AppPublics#EXTRA_UI_LOAD_ACTION_LOADING} key to the {@link Intent}.
   */
  public static String UI_LOAD_ACTION = "com.smartnsoft.droid4me.action.UI_LOADING";

  /**
   * A flag name which indicates whether the loading event is starting or ending.
   */
  public final static String EXTRA_UI_LOAD_ACTION_LOADING = "loading";

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
   * 
   * <p>
   * Is invoked by the framework, at initialization time.
   * </p>
   */
  static void initialize(Application application)
  {
    AppPublics.UI_LOAD_ACTION = application.getPackageName() + ".action.UI_LOADING";
    AppPublics.UPDATE_ACTION = application.getPackageName() + ".action.UPDATE";
    AppPublics.RELOAD_ACTION = application.getPackageName() + ".action.RELOAD";
    AppPublics.MultiSelectionHandler.SELECTION_ACTION = application.getPackageName() + ".action.SELECTION";
  }

  /**
   * Because an Android {@link Activity} can be destroyed and then recreated, following a configuration change (a screen orientation change, for
   * instance), this interface gives information about an entity life cycle.
   * 
   * @author �douard Mercier
   * @since 2010.01.05
   */
  public interface LifeCyclePublic
  {

    /**
     * Provides information about the current entity life cycle.
     * 
     * @return {@code true} if and only if the entity life cycle is the first time to execute during its container life
     */
    boolean isFirstLifeCycle();

    /**
     * Provides information about the current entity life cycle.
     * 
     * <p>
     * It is very handy when it comes to know whether the end-user can interact with the underlying activity.
     * </p>
     * 
     * @return {@code true} if and only if the underlying {@link Activity} life-cycle is between the {@link Activity#onResume()} and
     *         {@link Activity#onPause()} methods
     */
    boolean isInteracting();

    /**
     * Enables to know how many times the {@link LifeCycle#onSynchronizeDisplayObjects()} method has been invoked, which may be useful when you do not
     * want this method to do something that the {@link LifeCycle#onFulfillDisplayObjects()} method may have already done.
     * 
     * @return the number of time the {@link LifeCycle#onSynchronizeDisplayObjects()} method has actually been invoked
     */
    int getOnSynchronizeDisplayObjectsCount();

    /**
     * Indicates whether the extending {@link Activity} also implementing the {@link LifeCycle} interface is in the middle of a
     * {@link LifeCycle#refreshBusinessObjectsAndDisplay(boolean, Runnable)} call.
     * 
     * <p>
     * It is very handy when it comes to disable certain things, like menu entries, while an {@link Activity} is loading.
     * </p>
     * 
     * @return {@code true} if and only if the {@link LifeCycle#refreshBusinessObjectsAndDisplay(boolean, Runnable)} is being executed.
     */
    boolean isRefreshingBusinessObjectsAndDisplay();

  }

  /**
   * Indicates what kind of {@link Intent} are being listened to, and how to handle an intent.
   * 
   * @since 2010.02.04
   */
  public interface BroadcastListener
  {

    /**
     * @return if not {@code null}, only the intents that match with this returned value, will be received by the activity
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
   * <p>
   * This method will be invoked by the framework during the {@link Activity#onCreate()} method, which will register the underlying
   * {@link BroadcastReceiver}, and this receiver will be unregistered by the {@link Activity#onDestroy()} method.
   * </p>
   * 
   * @see AppPublics#BroadcastListenersProvider
   * @since 2010.02.04
   */
  public interface BroadcastListenerProvider
  {

    /**
     * @return the broadcast listener that this provider exposes ; can be {@code null}
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
   * A broadcast listener which listens only to {@link AppPublics#LOAD_ACTION loading intent actions}.
   * 
   * @since 2011.08.02
   */
  public static abstract class ReloadBroadcastListener
      implements AppPublics.BroadcastListener
  {

    private final Class<? extends Activity> activityClass;

    private final Class<?> componentClass;

    /**
     * Triggers a reload event through a {@linkplain Context#sendBroadcast() broadcast intent action}.
     * 
     * @param context
     *          the context which will be used to trigger the event
     * @param targetActivityClass
     *          the class which should receive the loading event
     * @param componentClass
     *          an optional (may be {@code null}) class which refines the component that should receive the event
     */
    public static void broadcastReload(Context context, Class<? extends Activity> targetActivityClass, Class<?> componentClass)
    {
      context.sendBroadcast(new Intent(AppPublics.RELOAD_ACTION).putExtra(AppPublics.EXTRA_ACTION_ACTIVITY, targetActivityClass.getName()).addCategory(
          targetActivityClass.getName()).putExtra(AppPublics.EXTRA_ACTION_COMPONENT, componentClass == null ? null : componentClass.getName()));
    }

    /**
     * Creates an {@link IntentFilter} which is able to listen for a broadcast reload event.
     * 
     * @param intentFilter
     *          an already existing intent filter, which will be enriched
     * @param targetActivityClass
     *          the class which is supposed to receive the broadcast event
     * @param componentClass
     *          an optional (may be {@code null}) class which refines the component that is supposed to receive the event
     * @return the provided intent filter, which has been enriched
     */
    public static IntentFilter addReload(IntentFilter intentFilter, Class<? extends Activity> targetActivityClass, Class<?> componentClass)
    {
      intentFilter.addAction(AppPublics.RELOAD_ACTION);
      intentFilter.addCategory(targetActivityClass.getName());
      return intentFilter;
    }

    /**
     * Indicates whether an intent matches a reload broadcast event.
     * 
     * @param intent
     *          the intent that has been received and which is to be analyzed
     * @param targetActivityClass
     *          the class which is supposed to receive the broadcast event
     * @param componentClass
     *          an optional (may be {@code null}) class which refines the component that is supposed to receive the event
     * @return {@code true} if and only if the intent matches the expected event
     */
    public static boolean matchesReload(Intent intent, Class<? extends Activity> targetActivityClass, Class<?> componentClass)
    {
      return intent.getAction() != null && intent.getAction().equals(AppPublics.RELOAD_ACTION) && intent.hasExtra(AppPublics.EXTRA_ACTION_ACTIVITY) == true && intent.getStringExtra(
          AppPublics.EXTRA_ACTION_ACTIVITY).equals(targetActivityClass.getName()) == true;
    }

    /**
     * Equivalent to {@link ReloadBroadcastListener#ReloadBroadcastListener(Class, Class) ReloadBroadcastListener(Class, null)}
     */
    public ReloadBroadcastListener(Class<? extends Activity> activityClass)
    {
      this(activityClass, null);
    }

    public ReloadBroadcastListener(Class<? extends Activity> activityClass, Class<?> componentClass)
    {
      this.activityClass = activityClass;
      this.componentClass = componentClass;
    }

    public IntentFilter getIntentFilter()
    {
      final IntentFilter intentFilter = new IntentFilter();
      ReloadBroadcastListener.addReload(intentFilter, activityClass, componentClass);
      return intentFilter;
    }

    public void onReceive(Intent intent)
    {
      if (ReloadBroadcastListener.matchesReload(intent, activityClass, componentClass) == true)
      {
        onReload();
      }
    }

    /**
     * The callback that will be triggered if the {@link AppPublics#RELOAD_ACTION} action is caught for the provided {@link Activity} class.
     */
    protected abstract void onReload();

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

    /**
     * Triggers a loading event through a {@linkplain Context#sendBroadcast() broadcast intent action}.
     * 
     * @param context
     *          the context which will be used to trigger the event
     * @param targetActivityClass
     *          the class which should receive the loading event
     * @param isLoading
     *          whether this deals with a loading which starts or stop
     * @param addCategory
     *          whether the broadcast intent should contain the target category
     */
    public static void broadcastLoading(Context context, Class<? extends Activity> targetActivityClass, boolean isLoading, boolean addCategory)
    {
      final Intent intent = new Intent(AppPublics.UI_LOAD_ACTION).putExtra(AppPublics.EXTRA_UI_LOAD_ACTION_LOADING, isLoading).putExtra(
          AppPublics.EXTRA_ACTION_ACTIVITY, targetActivityClass.getName());
      if (addCategory == true)
      {
        intent.addCategory(targetActivityClass.getName());
      }
      context.sendBroadcast(intent);
    }

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

    protected final Activity getActivity()
    {
      return activity;
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
      if (intent.getAction().equals(AppPublics.UI_LOAD_ACTION) == true && intent.hasExtra(AppPublics.EXTRA_ACTION_ACTIVITY) == true && intent.getStringExtra(
          AppPublics.EXTRA_ACTION_ACTIVITY).equals(activity.getClass().getName()) == true)
      {
        final int previousCounter = counter;
        // We only take into account the loading event coming from the activity itself
        final boolean isLoading = intent.getBooleanExtra(AppPublics.EXTRA_UI_LOAD_ACTION_LOADING, true);
        counter += (isLoading == true ? 1 : -1);

        // We only trigger an event provided the cumulative loading status has changed
        if (previousCounter == 0 && counter >= 1)
        {
          onLoading(true);
        }
        else if (previousCounter >= 1 && counter <= 0)
        {
          onLoading(false);
        }
      }
    }

    /**
     * Is invoked when a new loading event (intent) occurs.
     * 
     * @param isLoading
     *          is equal to {@code true} if and only if the underlying activity is not loading anymore
     */
    protected abstract void onLoading(boolean isLoading);

  }

  /**
   * A helper which receives mutli-selection {@link Intent intents} with an action set to {@link MultiSelectionHandler#SELECTION_ACTION}, and which
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
     * @see use the {@link MultiSelectionHandler#EXTRA_SELECTED} and {@link MultiSelectionHandler#EXTRA_BUSINESS_OBJECT} extra flags for indicated
     *      whether the business object is selected, and what business object this is about
     */
    public static String SELECTION_ACTION = "com.smartnsoft.droid4me.action.SELECTION";

    /**
     * Used as a key in the {@link Intent#getExtras() intent bundle}, so as to indicate whether the event deals with a selection or deselection.
     */
    public final static String EXTRA_SELECTED = "selected";

    /**
     * Used as a key in the {@link Intent#getExtras() intent bundle}, so as to indicate in the event the selected or deselected business.
     */
    public final static String EXTRA_BUSINESS_OBJECT = "businessObject";

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
     * The method to invoke when the activity receives a {@link MultiSelectionHandler#SELECTION_ACTION intent}, due to as business object
     * selection/unselection.
     * 
     * <p>
     * Usually invoked from the {@link BroadcastReceiver} which is listening to business objects selection events}.
     * </p>
     * 
     * @param intent
     *          the received intent; if the action of the Intent is not the right one, no processing is done
     * @param onMultiSelectionChanged
     *          the interface that will be callbacked (in the same thread as the calling method) depending on the overall multi-selection state; is
     *          allowed to be {@code null}
     * @return {@code true} if the intent has been handled ; {@code false} otherwise
     */
    @SuppressWarnings({ "unchecked" })
    public boolean onSelection(Intent intent, @SuppressWarnings("rawtypes") MultiSelectionHandler.OnMultiSelectionChanged onMultiSelectionChanged)
    {
      if (intent.getAction().equals(AppPublics.MultiSelectionHandler.SELECTION_ACTION) == false)
      {
        return false;
      }
      final boolean selected = intent.getBooleanExtra(MultiSelectionHandler.EXTRA_SELECTED, false) == true;
      final int previousSelectedCount = selectedCount;
      final BusinessObjectClass businessObject = (BusinessObjectClass) intent.getSerializableExtra(MultiSelectionHandler.EXTRA_BUSINESS_OBJECT);
      setSelection(businessObject, selected);
      if (onMultiSelectionChanged != null)
      {
        onMultiSelectionChanged.onSelectionChanged(previousSelectedCount >= 1, selectedCount >= 1, selectedObjects);
      }
      return true;
    }

    /**
     * Sets the selection state for a given business object.
     * 
     * <p>
     * However, the call does not trigger the
     * {@link AppPublics.MultiSelectionHandler.OnMultiSelectionChanged#onSelectionChanged(boolean, boolean, List)} callback.
     * </p>
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
   * There is no reason creating an instance of that class, which is just a container.
   */
  private AppPublics()
  {
  }

}
