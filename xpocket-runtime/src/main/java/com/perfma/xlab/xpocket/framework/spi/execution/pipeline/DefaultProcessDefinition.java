package com.perfma.xlab.xpocket.framework.spi.execution.pipeline;

import com.perfma.xlab.xpocket.command.impl.SysCommand;
import com.perfma.xlab.xpocket.framework.spi.impl.XPocketStatusContext;
import com.perfma.xlab.xpocket.spi.command.XPocketCommand;
import com.perfma.xlab.xpocket.spi.context.PluginBaseInfo;

import java.io.OutputStream;

/**
 * @author gongyu <yin.tong@perfma.com>
 */
public class DefaultProcessDefinition {

    protected final PluginBaseInfo context;
    protected final String cmd;
    protected final String[] args;
    protected boolean isEnd = true;

    protected boolean hasProcess = false;
    protected DefaultXPocketProcess currentProcess;

    protected DefaultProcessDefinition next;

    protected OutputStream outputStream;

    protected ExecutionPipeLine pipeline;

    public DefaultProcessDefinition(PluginBaseInfo context, String cmd, String[] args) {
        this.context = context;
        cmd = cmd.replace(context.getName() + ".", "");
        cmd = cmd.replace("@" + context.getNamespace().toUpperCase(), "");
        cmd = cmd.replace("@" + context.getNamespace().toLowerCase(), "");
        this.cmd = cmd;
        this.args = args;
    }

    public void setDefaultEnd(boolean isEnd) {
        this.isEnd = isEnd;
    }

    public void setNext(DefaultProcessDefinition next) {
        this.next = next;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setPipeline(ExecutionPipeLine pipeline) {
        this.pipeline = pipeline;
    }

    public void execute(String input) throws Throwable {
        String[] realArgs;
        if (input == null || input.trim().isEmpty()) {
            realArgs = args;
        } else {
            if (context.getCommand(cmd) != null && context.getCommand(cmd).isPiped()) {
                realArgs = args;
            } else {
                realArgs = new String[args.length + 1];
                realArgs[0] = input;
                System.arraycopy(args, 0, realArgs, 1, args.length);
            }
        }

        DefaultXPocketProcess process = new DefaultXPocketProcess(cmd, realArgs);
        process.setInput(input);
        process.setOutputStream(outputStream);
        process.setPdef(this);
        currentProcess = process;
        hasProcess = true;
        XPocketCommand command = context.getCommand(cmd);
        if (command == null) {
            //未找到的命令尝试交给系统命令
            command = SysCommand.getInstance();
        }
        command.invoke(process, XPocketStatusContext.instance);
    }

    public void pipeEnd() {
        this.isEnd = true;
        if (!hasProcess) {
            end();
        }
    }

    public void end() {
        hasProcess = false;
        currentProcess = null;
        if (pipeline != null && isEnd) {
            pipeline.end();
        } else if (pipeline == null && isEnd) {
            next.pipeEnd();
        }
    }

    public void interrupt() {
        if (hasProcess) {
            currentProcess.interrupt();
        }

        if (next != null) {
            next.interrupt();
        }
    }

    public void userInput(String input) {
        if (hasProcess) {
            currentProcess.userInput(input);
        }

        if (next != null) {
            next.userInput(input);
        }
    }

}
