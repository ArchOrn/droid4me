/*
 * (C) Copyright 2009-2016 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.support.v7.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.app.SmartableActivity;
import com.smartnsoft.droid4me.support.v4.app.SmartFragmentActivity;

/**
 * @author Jocelyn Girard
 * @since 2014.10.29
 */
public abstract class SmartAppCompatActivity<AggregateClass>
    extends AppCompatActivity
    implements SmartableActivity<AggregateClass>
{

  private final Droid4mizer<AggregateClass, SmartAppCompatActivity<AggregateClass>> droid4mizer = new Droid4mizer<>(this, this, this, null);

  @Override
  public LayoutInflater getLayoutInflater()
  {
    return (LayoutInflater) droid4mizer.getSystemService(Context.LAYOUT_INFLATER_SERVICE, super.getLayoutInflater());
  }

  @Override
  public Object getSystemService(String name)
  {
    if (Context.LAYOUT_INFLATER_SERVICE.equals(name) == true && getWindow() != null)
    {
      return droid4mizer.getSystemService(name, getWindow().getLayoutInflater());
    }
    else
    {
      return droid4mizer.getSystemService(name, super.getSystemService(name));
    }
  }

  @Override
  public void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    droid4mizer.onAttached(this);
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      @Override
      public void run()
      {
        SmartAppCompatActivity.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);
    droid4mizer.onPostCreate(savedInstanceState);
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    droid4mizer.onNewIntent(intent);
  }

  // @Override
  // public void onContentChanged()
  // {
  // super.onContentChanged();
  // droid4mizer.onContentChanged();
  // }

  @Override
  protected void onResume()
  {
    super.onResume();
    droid4mizer.onResume();
  }

  @Override
  protected void onPostResume()
  {
    super.onPostResume();
    droid4mizer.onPostResume();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    droid4mizer.onConfigurationChanged(newConfig);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    droid4mizer.onSaveInstanceState(outState);
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    droid4mizer.onStart();
  }

  @Override
  protected void onRestart()
  {
    super.onRestart();
    droid4mizer.onRestart();
  }

  @Override
  protected void onPause()
  {
    try
    {
      droid4mizer.onPause();
    }
    finally
    {
      super.onPause();
    }
  }

  @Override
  protected void onStop()
  {
    try
    {
      droid4mizer.onStop();
    }
    finally
    {
      super.onStop();
    }
  }

  @Override
  protected void onDestroy()
  {
    try
    {
      droid4mizer.onDestroy();
    }
    finally
    {
      super.onDestroy();
    }
  }

  @Override
  public void onDetachedFromWindow()
  {
    try
    {
      droid4mizer.onDetached();
    }
    finally
    {
      super.onDetachedFromWindow();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Taken from http://www.londatiga.net/it/android-coding-tips-how-to-create-options-menu-on-child-activity-inside-an-activitygroup/
    return droid4mizer.onCreateOptionsMenu(getParent() == null ? super.onCreateOptionsMenu(menu) : true, menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    return droid4mizer.onPrepareOptionsMenu(getParent() == null ? super.onPrepareOptionsMenu(menu) : true, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    return droid4mizer.onOptionsItemSelected(getParent() == null ? super.onOptionsItemSelected(item) : true, item);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    return droid4mizer.onContextItemSelected(getParent() == null ? super.onContextItemSelected(item) : true, item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    droid4mizer.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * SmartableActivity implementation.
   */

  /**
   * Smartable implementation.
   */

  @Override
  public AggregateClass getAggregate()
  {
    return droid4mizer.getAggregate();
  }

  @Override
  public void setAggregate(AggregateClass aggregate)
  {
    droid4mizer.setAggregate(aggregate);
  }

  @Override
  public Handler getHandler()
  {
    return droid4mizer.getHandler();
  }

  @Override
  public SharedPreferences getPreferences()
  {
    return droid4mizer.getPreferences();
  }

  @Override
  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    droid4mizer.onException(throwable, fromGuiThread);
  }

  @Override
  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    droid4mizer.registerBroadcastListeners(broadcastListeners);
  }

  @Override
  public int getOnSynchronizeDisplayObjectsCount()
  {
    return droid4mizer.getOnSynchronizeDisplayObjectsCount();
  }

  @Override
  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return droid4mizer.isRefreshingBusinessObjectsAndDisplay();
  }

  @Override
  public boolean isFirstLifeCycle()
  {
    return droid4mizer.isFirstLifeCycle();
  }

  @Override
  public final boolean isInteracting()
  {
    return droid4mizer.isInteracting();
  }

  @Override
  public final boolean isAlive()
  {
    return droid4mizer.isAlive();
  }

  @Override
  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
  {
    droid4mizer.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately);
  }

  /**
   * AppInternals.LifeCycleInternals implementation.
   */

  @Override
  public boolean shouldKeepOn()
  {
    return droid4mizer.shouldKeepOn();
  }

  /**
   * Own implementation.
   */

  @Override
  public void onBusinessObjectsRetrieved()
  {
  }

  /**
   * Same as invoking {@code refreshBusinessObjectsAndDisplay(true, null, false)}.
   *
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

}
