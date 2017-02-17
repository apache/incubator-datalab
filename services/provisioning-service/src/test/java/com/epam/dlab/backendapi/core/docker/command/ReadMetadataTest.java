/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.core.docker.command;

import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.dlab.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class ReadMetadataTest {
    private static final String EMR_METADATA_DESCRIPTION_JSON = "/metadata/description.json";
    private static final String JUPITER_METADATA_DESCRIPTION_JSON = "/metadata/description_1.json";


    @Test
    public void readEmrMetadataTest() throws IOException, URISyntaxException {
        ImageMetadataDTO imageMetadataDTO =
                ResourceUtils.readResourceAsClass(getClass(),
                        EMR_METADATA_DESCRIPTION_JSON,
                        ComputationalMetadataDTO.class);

        Assert.assertNotNull(imageMetadataDTO);
    }

    @Test
    public void readJupiterMetadataTest() throws IOException, URISyntaxException {
        ImageMetadataDTO imageMetadataDTO =
                ResourceUtils.readResourceAsClass(getClass(),
                    JUPITER_METADATA_DESCRIPTION_JSON,
                ExploratoryMetadataDTO.class);
        Assert.assertNotNull(imageMetadataDTO);
    }
}
