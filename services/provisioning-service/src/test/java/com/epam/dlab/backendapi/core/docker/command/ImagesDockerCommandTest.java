/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core.docker.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImagesDockerCommandTest {

    String GET_IMAGES = "docker images | awk '{print $1\":\"$2}' | sort | uniq | grep \"dlab\" | grep -v \"none\" | grep -v \"edge\"";

    @Test
    public void testBuildGetImagesCommand() {
        String getImagesCommand = new ImagesDockerCommand()
                .pipe(UnixCommand.awk("{print $1\":\"$2}"))
                .pipe(UnixCommand.sort())
                .pipe(UnixCommand.uniq())
                .pipe(UnixCommand.grep("dlab"))
                .pipe(UnixCommand.grep("none", "-v"))
                .pipe(UnixCommand.grep("edge", "-v"))
                .toCMD();
        assertEquals(GET_IMAGES, getImagesCommand);
    }
}
