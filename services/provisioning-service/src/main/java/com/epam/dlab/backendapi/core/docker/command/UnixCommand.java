/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core.docker.command;

public class UnixCommand {
    private String command;

    public UnixCommand(String command) {
        this.command = command;
    }

    public static UnixCommand awk(String txt) {
        return new UnixCommand("awk '" + txt + "'");
    }

    public static UnixCommand sort() {
        return new UnixCommand("sort");
    }

    public static UnixCommand uniq() {
        return new UnixCommand("uniq");
    }

    public static UnixCommand grep(String searchFor, String... options) {
        StringBuilder sb = new StringBuilder("grep");
        for (String option : options) {
            sb.append(' ').append(option);
        }
        sb.append(" \"" + searchFor + "\"");
        return new UnixCommand(sb.toString());
    }

    public String getCommand() {
        return command;
    }
}