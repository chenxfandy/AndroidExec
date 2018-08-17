# AndroidExec

Android命令执行以及回调

## 使用

```
// 初始化，默认：不限制并发线程数；指令超时10s终止
Commander.init();

// 自定义初始化参数：超时1s终止
Commander.init(new CommanderOptions.Builder().setTimeOut(1000).build())

// 创建执行命令
Commander.addTask("ls", new IListener() {
    @Override
    public void onPre(String command) {
        Log.i(TAG, "onPre: " + command);
    }

    @Override
    public void onProgress(String message) {
        Log.i(TAG, "onProgress: " + message);
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public void onSuccess(String message) {
        Log.i(TAG, "onSuccess: " + message);
    }
});

// 终止命令
CommandTask.discard()

// 终止所有命令
Commander.destory()
```

## Runtime

```
// 执行命令
Process process = Runtime.getRuntime().exec("command");

// 读取正常输出
process.getInputStream()

// 读取错误输出
process.getErrorStream()
```

## ProcessBuilder

```
// 执行命令，重定向输出流
Process process = new ProcessBuilder("command").command("arg").redirectErrorStream(true).start();

// 不设置重定向，则正常输出、错误输出如同Runtime；
// 设置了重定向后，正常输出、错误输出都统一读取process.getInputStream()
```

**注意**

**ProcessBuilder.start() 和 Runtime.exec()传递的参数有所不同，Runtime.exec()可接受一个单独的字符串，这个字符串是通过空格来分隔可执行命令程序和参数的；也可以接受字符串数组参数。而ProcessBuilder的构造函数是一个字符串列表或者数组。列表中第一个参数是可执行命令程序，其他的是命令行执行是需要的参数。通过查看JDK源码可知，Runtime.exec最终是通过调用ProcessBuilder来真正执行操作的。**
