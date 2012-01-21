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

import android.view.View;
import android.widget.ListView;

/**
 * An instantiation of the {@link AbstractSmartListActivity} bound to the native {@link ListView}.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @param <BusinessObjectClass>
 *          the business objects being handled
 * @param <ViewClass>
 *          the {@link View} representation of the business objects
 * 
 * @author �douard Mercier
 * @since 2011.06.15
 */
public abstract class SmartListActivity<AggregateClass, BusinessObjectClass, ViewClass extends View>
    extends AbstractSmartListActivity<AggregateClass, BusinessObjectClass, ListView, ViewClass>
{
}