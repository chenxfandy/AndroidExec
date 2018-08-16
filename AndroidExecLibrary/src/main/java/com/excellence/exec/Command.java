package com.excellence.exec;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     time   : 2018/8/16
 *     desc   :
 * </pre> 
 */
public class Command {

    private static final String TAG = Command.class.getSimpleName();

    protected static final int DEFAULT_TIME_OUT = 10 * 1000;

    private final LinkedList<CommandTask> mTaskQueue;
    private int mParallelTaskCount = 0;
    private int mTimeOut = 0;

    protected Command(int parallelTaskCount, int timeOut) {
        mTaskQueue = new LinkedList<>();
        mParallelTaskCount = parallelTaskCount;
        if (mParallelTaskCount <= 0) {
            mParallelTaskCount = Integer.MAX_VALUE;
        }
        mTimeOut = timeOut;
        if (mTimeOut <= 0) {
            mTimeOut = DEFAULT_TIME_OUT;
        }
    }

    public CommandTask addTask(List<String> command, IListener listener) {
        CommandTask task = new CommandTask(command, listener);
        synchronized (mTaskQueue) {
            mTaskQueue.add(task);
        }
        schedule();
        return task;
    }

    private synchronized void schedule() {
        // count running task
        int runningTaskCount = 0;
        for (CommandTask task : mTaskQueue) {
            if (task.isRunning) {
                runningTaskCount++;
            }
        }

        if (runningTaskCount >= mParallelTaskCount) {
            return;
        }

        // deploy task to fill parallel task count
        for (CommandTask task : mTaskQueue) {
            task.deploy();
            if (++runningTaskCount == mParallelTaskCount) {
                return;
            }
        }
    }

    private synchronized void remove(CommandTask task) {
        mTaskQueue.remove(task);
        schedule();
    }

    /**
     * 关闭所有下载任务
     */
    public synchronized void clearAll() {
        while (!mTaskQueue.isEmpty()) {
            mTaskQueue.get(0).cancel();
        }
    }

    public class CommandTask {

        private List<String> mCommand = null;
        private Process mProcess = null;
        private boolean isRunning = false;
        private IListener mIListener = null;

        private CommandTask(List<String> command, final IListener listener) {
            mCommand = command;
            mIListener = new IListener() {
                @Override
                public void onPre(String command) {
                    if (listener != null) {
                        listener.onPre(command);
                    }
                }

                @Override
                public void onProgress(String message) {
                    if (listener != null) {
                        listener.onProgress(message);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    isRunning = false;
                    if (listener != null) {
                        listener.onError(t);
                    }
                    schedule();
                }

                @Override
                public void onSuccess(String message) {
                    isRunning = false;
                    if (listener != null) {
                        listener.onSuccess(message);
                    }
                    remove(CommandTask.this);
                }
            };
        }

        void deploy() {
            try {
                isRunning = true;
                Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                        StringBuilder cmd = new StringBuilder();
                        for (String item : mCommand) {
                            cmd.append(item).append(" ");
                        }
                        mIListener.onPre(cmd.toString());
                        mProcess = new ProcessBuilder(mCommand).redirectErrorStream(true).start();

                        BufferedReader stdin = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
                        StringBuilder result = new StringBuilder();
                        String line = null;
                        while ((line = stdin.readLine()) != null) {
                            mIListener.onProgress(line);
                            result.append(line);
                        }
                        stdin.close();

                        mIListener.onSuccess(result.toString());
                    }
                }).observeOn(Schedulers.io()).subscribe();
            } catch (Exception e) {
                mIListener.onError(e);
            }
        }

        private void cancel() {
            if (mProcess != null) {
                mProcess.destroy();
            }
            mTaskQueue.remove(this);
        }

        public void discard() {
            if (mProcess != null) {
                mProcess.destroy();
            }
            remove(this);
        }

    }
}
