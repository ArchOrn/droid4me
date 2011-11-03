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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A container which encloses various types which enable to run commands in a detached thread.
 * 
 * @author Édouard Mercier
 * @since 2011.11.03
 */
public final class SmartCommands
{

  /**
   * A {@link Runnable} which is allowed to throw an exception during its execution.
   * 
   * <p>
   * During the command execution, any thrown {@link Throwable} will be delivered to the {@link ActivityController} through its
   * {@link ActivityController#handleException(Context, Throwable)} method, so that it can be controller in a central way, and not "swallowed".
   * </p>
   * 
   * <p>
   * It has been specifically designed for being able to run {@link Runnable} which throw exceptions within the {@link SmartThreadPoolExecutor}.
   * </p>
   * 
   * @since 2010.06.08
   */
  public abstract static class GuardedCommand
      implements Runnable
  {

    protected final static Logger log = LoggerFactory.getInstance(SmartCommands.GuardedCommand.class);

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

    /**
     * A fallback method which will be triggered if a {@link Throwable} is thrown during the {@link #runGuarded()} method, so as to let the caller a
     * chance to handle locally the exception.
     * 
     * <p>
     * By default, the method does nothing and returns the provided <code>throwable</code>.
     * </p>
     * 
     * @param throwable
     *          the exception that has been thrown during the {@link #runGuarded()} execution
     * @return <code>null</code> if and only if the method has handled the exception and that the {@link ActivityController.ExceptionHandler} should
     *         not be invoked ; otherwise, the {@link Throwable} that should be submitted to the {@link ActivityController.ExceptionHandler}
     */
    protected Throwable handle(Throwable throwable)
    {
      return throwable;
    }

    /**
     * This method will invoke the {@link #runGuarded()} method, and handle any thrown exception.
     */
    public final void run()
    {
      try
      {
        runGuarded();
      }
      catch (Throwable throwable)
      {
        // We let a chance to the caller to handle the exception
        final Throwable modifiedThrowable = handle(throwable);
        if (modifiedThrowable == null)
        {
          // In that case, the exception has been handled locally
          return;
        }
        // We handle the exception
        ActivityController.getInstance().handleException(context, modifiedThrowable);
      }
    }

  }

  /**
   * Enables to execute in background a task, by notifying when the execution is running. It is especially useful when the UI should be notified when
   * a command is running.
   * 
   * <p>
   * When the command is executed by the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL}, its underlying {@link ProgressHandler} is notified.
   * </p>
   * 
   * @since 2010.11.30
   */
  public static abstract class ProgressGuardedCommand
      extends SmartCommands.GuardedCommand
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
   * An exception which acts as an {@link Throwable} wrapper, and which works in combination with the {@link SmartCommands.SimpleGuardedCommmand}. It
   * will be triggered during the {@link SmartCommands.SimpleGuardedCommmand#handle(Throwable)} method.
   * 
   * <p>
   * The traditional case is to let the {@link ActivityController#registerExceptionHandler(ActivityController.ExceptionHandler) exception handler}
   * cope in a centralized way with this kind of exception.
   * </p>
   * 
   * @since 2011.11.03
   */
  public static class GuardedException
      extends Exception
  {

    private static final long serialVersionUID = 642514965027273713L;

    public final String displayMessage;

    /**
     * Builds a wrapper over the provided exception, and with an attached human readable message.
     * 
     * @param throwable
     *          the exception to be wrapped
     * @param displayMessage
     *          the supposedly i18ned message that will be used later on when actually handling that exception
     */
    protected GuardedException(Throwable throwable, String displayMessage)
    {
      super(throwable);
      this.displayMessage = displayMessage;
    }

  }

  /**
   * A handy {@link SmartCommands.GuardedCommand} which will issue systematically a log when an exception occurs during the command execution, and
   * will trigger a {@link SmartCommands.GuardedException} which wraps the original exception in that case.
   * 
   * @since 2011.11.03
   */
  public abstract static class SimpleGuardedCommmand
      extends SmartCommands.GuardedCommand
  {

    protected final String warningLogMessage;

    protected final String warningDisplayMessage;

    /**
     * Same as {@link SmartCommands.SimpleGuardedCommmand#SimpleGuardedCommmand(Context, String, String)} with the last parameter equal to
     * {@code context.getString(warningDisplayMessageResourceId)}.
     */
    public SimpleGuardedCommmand(Context context, String warningLogMessage, int warningDisplayMessageResourceId)
    {
      this(context, warningLogMessage, context.getString(warningDisplayMessageResourceId));
    }

    /**
     * Creates a new {@link SmartCommands.GuardedCommand}, which will issue a {@link Log#WARN warning log} and then trigger a
     * {@link SmartCommands.GuardedException}, if an exception occurs during its execution.
     * 
     * @param context
     *          the Android context under which the commands is being run
     * @param warningLogMessage
     *          the log message that will be output in case of exception
     * @param warningDisplayMessage
     *          the (supposedly i18ned) human readable that will be transfered to the {@link SmartCommands.GuardedException} in case of exception
     *          during the command execution
     */
    public SimpleGuardedCommmand(Context context, String warningLogMessage, String warningDisplayMessage)
    {
      super(context);
      this.warningLogMessage = warningLogMessage;
      this.warningDisplayMessage = warningDisplayMessage;
    }

    /**
     * The implementation will log as a {@link Log#WARN warning} the exception, and return a {@link SmartCommands.GuardedException} which wraps the
     * provided exception, with the {@link SmartCommands.SimpleGuardedCommmand#warningDisplayMessage} as
     * {@link SmartCommands.GuardedException#displayMessage message attribute}.
     * 
     * @see SmartCommands.GuardedCommand#handle(Throwable)
     */
    @Override
    protected Throwable handle(Throwable throwable)
    {
      if (log.isWarnEnabled())
      {
        log.warn(warningLogMessage, throwable);
      }
      return new SmartCommands.GuardedException(throwable, warningDisplayMessage);
    }

  }

  /**
   * A handy {@link SmartCommands.SimpleGuardedCommmand} which will issue systematically {@link DialogInterface#dismiss() dismiss} a
   * {@link ProgressDialog} once the command execution is over.
   * 
   * <p>
   * This kind of command is especially useful when a {@link ProgressDialog} is being displayed just before the current command execution, and that it
   * should be dismissed at the end of its execution.
   * </p>
   * 
   * @since 2011.11.03
   */
  public abstract static class ProgressDialogGuardedCommmand
      extends SmartCommands.SimpleGuardedCommmand
  {

    protected final ProgressDialog progressDialog;

    /**
     * Same as {@link SmartCommands.ProgressDialogGuardedCommmand#ProgressDialogGuardedCommmand(Context, String, String, ProgressDialog)} with the
     * third parameter equal to {@code context.getString(warningDisplayMessageResourceId)}.
     */
    public ProgressDialogGuardedCommmand(Context context, String warningLogMessage, int warningDisplayMessageResourceId, ProgressDialog progressDialog)
    {
      this(context, warningLogMessage, context.getString(warningDisplayMessageResourceId), progressDialog);
    }

    /**
     * 
     * Creates a new {@link SmartCommands.GuardedCommand}, which will issue a {@link Log#WARN warning log} and then trigger a
     * {@link SmartCommands.SimpleGuardedCommmand}, if an exception occurs during its execution, and eventually {@link DialogInterface#dismiss
     * dismiss} the provided dialog.
     * 
     * @param context
     *          the Android context under which the commands is being run
     * @param warningLogMessage
     *          the log message that will be output in case of exception
     * @param warningDisplayMessage
     *          the (supposedly i18ned) human readable that will be transfered to the {@link SmartCommands.GuardedException} in case of exception
     *          during the command execution
     * @param progressDialog
     *          the dialog to be dismissed at the end of the command execution ; may be {@code null}, and in that case, just behaves as its parent
     *          {@link SmartCommands.SimpleGuardedCommmand}
     * @see SmartCommands.SimpleGuardedCommmand#SimpleGuardedCommmand(Context, String, String)
     */
    public ProgressDialogGuardedCommmand(Context context, String warningLogMessage, String warningDisplayMessage, ProgressDialog progressDialog)
    {
      super(context, warningLogMessage, warningDisplayMessage);
      this.progressDialog = progressDialog;
    }

    /**
     * The actual command method to implement.
     * 
     * @throws Exception
     *           if something wrong happened during the command execution
     */
    protected abstract void runGuardedDialog()
        throws Exception;

    /**
     * The implementation will invoke the {@link #runGuardedDialog()} method, and will eventually dismiss the {@link #progressDialog} if necessary,
     * whatever happens.
     * 
     * @see SmartCommands.SimpleGuardedCommmand#runGuarded()
     */
    @Override
    protected final void runGuarded()
        throws Exception
    {
      try
      {
        runGuardedDialog();
      }
      finally
      {
        // This can be done from any thread, according to the documentation
        if (progressDialog != null && progressDialog.isShowing() == true)
        {
          if (getContext() instanceof Activity)
          {
            // We want to prevent from dismissing the ProgressDialog once its creating Activity is already finished, and hence prevent from a crash
            final Activity activity = (Activity) getContext();
            if (activity.isFinishing() == true)
            {
              return;
            }
          }
          progressDialog.dismiss();
        }
      }
    }

  }

  /**
   * A {@link DialogInterface.OnClickListener} which runs its {@link SmartCommands.GuardedCommand#runGuarded() execution} in the
   * {@link SmartCommands#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   * 
   * @since 2010.06.08
   */
  public static abstract class GuardedDialogInterfaceClickListener
      extends SmartCommands.GuardedCommand
      implements DialogInterface.OnClickListener
  {

    /**
     * @param activity
     *          the activity from which the execution originates, and which will be used when reporting a potential exception
     */
    public GuardedDialogInterfaceClickListener(Activity activity)
    {
      super(activity);
    }

    public final void onClick(DialogInterface dialog, int which)
    {
      SmartCommands.execute(this);
    }

  }

  /**
   * A {@link View.OnClickListener} which runs its {@link SmartCommands.GuardedCommand#runGuarded() execution} in the
   * {@link SmartCommands#LOW_PRIORITY_THREAD_POOL low-priority threads pool}, and which handles exceptions.
   * 
   * @since 2010.06.08
   */
  public static abstract class GuardedViewClickListener
      extends SmartCommands.GuardedCommand
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
      SmartCommands.execute(this);
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

    public SmartThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
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
    public void execute(SmartCommands.GuardedCommand guardedCommand)
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

  /**
   * An instance counter for the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL thread pool}.
   */
  private static int lowPriorityThreadCount = 1;

  /**
   * Indicates how many threads at most will be available in the {@link #LOW_PRIORITY_THREAD_POOL low-priority threads pool}, by default. It needs to
   * be sent at the application start-up.
   * 
   * <p>
   * You may change that pool size by invoking the {@link ThreadPoolExecutor#setCorePoolSize(int)} method.
   * </p>
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
   */
  public final static SmartCommands.SmartThreadPoolExecutor LOW_PRIORITY_THREAD_POOL = new SmartCommands.SmartThreadPoolExecutor(SmartCommands.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, SmartCommands.LOW_PRIORITY_THREAD_POOL_DEFAULT_SIZE, 10l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.setName("droid4me-lowpool-thread #" + SmartCommands.lowPriorityThreadCount++);
      return thread;
    }
  });

  /**
   * Simply executes the provided command via the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL}.
   * 
   * <p>
   * Equivalent to invoking {@link SmartCommands#LOW_PRIORITY_THREAD_POOL#execute(SmartCommands.GuardedCommand)}.
   * </p>
   * 
   * @param guardedCommand
   *          the command to be executed
   * @see #execute(Activity, Runnable)
   */
  public static void execute(SmartCommands.GuardedCommand guardedCommand)
  {
    SmartCommands.LOW_PRIORITY_THREAD_POOL.execute(guardedCommand);
  }

  /**
   * Equivalent to invoking {@link SmartCommands#LOW_PRIORITY_THREAD_POOL#execute(Activity, Runnable)}.
   * 
   * @see #execute(SmartCommands.GuardedCommand)
   */
  public static void execute(Activity activity, Runnable runnable)
  {
    SmartCommands.LOW_PRIORITY_THREAD_POOL.execute(activity, runnable);
  }

  /**
   * There is no reason creating an instance of that class, which is just a container.
   */
  private SmartCommands()
  {
  }

}
