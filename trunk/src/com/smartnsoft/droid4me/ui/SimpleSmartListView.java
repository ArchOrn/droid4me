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

package com.smartnsoft.droid4me.ui;

import com.smartnsoft.droid4me.framework.DetailsProvider.ForList;

import android.app.Activity;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout.LayoutParams;

/**
 * A simple (not expandable) smart list view.
 * 
 * @author �douard Mercier
 * @since 2008.11.14
 */
public class SimpleSmartListView<BusinessObjectClass, ViewClass extends View>
    extends SmartListView<BusinessObjectClass, ViewClass>
{

  // Inspired from http://groups.google.com/group/android-developers/browse_thread/thread/59dbe46cfbc5672f/e3a5e21754a7a725?lnk=raot
  private final class TheListView
      extends ListView
      implements OnGestureListener
  {
    private final GestureDetector gestureDetector;

    public TheListView(Context context)
    {
      super(context);
      setId(android.R.id.list);
      gestureDetector = new GestureDetector(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
      // log.debug("onTouchEvent(" + event + ")");
      if (gestureDetector.onTouchEvent(event) == true)
      {
        // log.debug("onTouchEvent() returns true");
        return true;
      }
      return super.onTouchEvent(event);
    }

    public boolean onDown(MotionEvent event)
    {
      return false;
    }

    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY)
    {
      // log.debug("onFling(" + event1 + "," + event2 + "," + velocityX + "," + velocityY + ")");
      if (getListView().isEnabled() == true && Math.abs(velocityX) > 1500 && onEventObjectListener != null && event1 != null)
      {
        final int position = listView.pointToPosition((int) event1.getX(), (int) event1.getY());
        if (position >= 0)
        {
          // This is the only that could be found in order to find back the associated view
          final BusinessObjectClass businessObject = getFilteredObjects().get(position);
          for (int index = 0; index < listView.getChildCount(); index++)
          {
            final int positionForView = listView.getPositionForView(listView.getChildAt(index));
            if (positionForView == position)
            {
              return onEventObjectListener.onWipedObject(listView.getChildAt(index), businessObject, velocityX > 0);
            }
          }
        }
      }
      return false;
    }

    public void onLongPress(MotionEvent event)
    {
    }

    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY)
    {
      return false;
    }

    public void onShowPress(MotionEvent event)
    {
    }

    public boolean onSingleTapUp(MotionEvent event)
    {
      return false;
    }

  };

  private final ListView listView;

  private BaseAdapter adapter;

  private final ForList<BusinessObjectClass, ViewClass> forListProvider;

  public SimpleSmartListView(Activity activity, ForList<BusinessObjectClass, ViewClass> forListProvider)
  {
    super(activity);
    this.forListProvider = forListProvider;
    listView = new TheListView(activity);
  }

  @Override
  protected ForList<BusinessObjectClass, ViewClass> getForListProvider()
  {
    return forListProvider;
  }

  @Override
  public ListView getListView()
  {
    return listView;
  }

  @Override
  public void setHeaderFooterView(boolean top, boolean fixed, View view)
  {
    if (top == true)
    {
      if (fixed == true)
      {
        headerLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
      }
      else
      {
        listView.addHeaderView(view);
      }
    }
    else
    {
      if (fixed == true)
      {
        footerLayout.addView(view, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
      }
      else
      {
        listView.addFooterView(view);
      }
    }
  }

  public void setEmptyView(View view)
  {
    if (listView.getEmptyView() != null)
    {
      listWrapperLayout.removeView(listView.getEmptyView());
    }
    listView.setEmptyView(view);
    if (view != null)
    {
      listWrapperLayout.addView(view, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    }
  }

  @Override
  public void invalidateViews()
  {
    listView.invalidateViews();
  }

  @Override
  public final void setAdapter(BaseAdapter adapter)
  {
    this.adapter = adapter;
  }

  @Override
  protected void setAdapter()
  {
    // TODO: remove the duplicated "setOnItemClickListener"
    listView.setOnItemClickListener(new OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId)
      {
        if (getListView().isEnabled() == false)
        {
          return;
        }
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (actualPosition < 0)
        {
          return;
        }
        synchronized (filteredObjectsSynchronization)
        {
          if (actualPosition >= getFilteredObjects().size())
          {
            if (log.isErrorEnabled())
            {
              log.error("The selected row " + actualPosition + " exceeds the size of the filtered business objetcs list which is " + getFilteredObjects().size());
            }
            return;
          }
          if (onEventObjectListener != null)
          {
            onEventObjectListener.onClickedObject(view, getFilteredObjects().get(actualPosition));
          }
        }
      }
    });
    listView.setOnItemLongClickListener(new OnItemLongClickListener()
    {
      public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long rowId)
      {
        if (getListView().isEnabled() == false)
        {
          return false;
        }
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
        {
          return false;
        }
        setSelectedObject(getFilteredObjects().get(actualPosition));
        return false;
      }
    });
    final OnItemClickListener onItemClickListener = listView.getOnItemClickListener();
    listView.setOnItemClickListener(new OnItemClickListener()
    {
      public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
      {
        if (getListView().isEnabled() == false)
        {
          return;
        }
        final int actualPosition = position - listView.getHeaderViewsCount();
        if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
        {
          return;
        }
        setSelectedObject(getFilteredObjects().get(actualPosition));
        onItemClickListener.onItemClick(arg0, arg1, position, arg3);
      }
    });
    listView.setOnItemSelectedListener(new OnItemSelectedListener()
    {
      public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId)
      {
        final int actualPosition = position - listView.getHeaderViewsCount();
        synchronized (filteredObjectsSynchronization)
        {
          if (getFilteredObjects() == null)
          {
            if (log.isErrorEnabled())
            {
              log.error("The row at position " + position + " has been marked as selected whereas no filter business objects are available yet");
            }
            return;
          }

          if (actualPosition < 0 || actualPosition >= getFilteredObjects().size())
          {
            // No item is selected
            setSelectedObject(null);
            return;
          }
          setSelectedObject(getFilteredObjects().get(actualPosition));
          if (onEventObjectListener != null)
          {
            onEventObjectListener.onSelectedObject(view, getSelectedObject());
          }
          // log.debug("The view at position " + actualPosition + " is now selected, and corresponds to the business object with id '" +
          // forListProvider.getObjectId(getSelectedObject()) + "'");
        }
      }

      @SuppressWarnings("unchecked")
      public void onNothingSelected(AdapterView adapterView)
      {
        setSelectedObject(null);
      }
    });
    listView.setAdapter(adapter);
  }

  @Override
  public void notifyDataSetChanged(boolean businessObjectCountAndSortingUnchanged)
  {
    if (businessObjectCountAndSortingUnchanged == false)
    {
      adapter.notifyDataSetInvalidated();
    }
    else
    {
      adapter.notifyDataSetChanged();
    }
  }

  @Override
  protected void setSelected(int position)
  {
    listView.setSelection(position + listView.getHeaderViewsCount());
  }

}
