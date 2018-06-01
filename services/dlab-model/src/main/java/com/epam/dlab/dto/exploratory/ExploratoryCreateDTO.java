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

package com.epam.dlab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.List;

public class ExploratoryCreateDTO<T extends ExploratoryCreateDTO<?>> extends ExploratoryBaseDTO<T> {

    @SuppressWarnings("unchecked")
    private final T self = (T) this;

    @JsonProperty("git_creds")
    private List<ExploratoryGitCreds> gitCreds;
    @JsonProperty("notebook_image_name")
    private String imageName;

    /**
     * Return the list of GIT credentials.
     */
    public List<ExploratoryGitCreds> getGitCreds() {
        return gitCreds;
    }

    /**
     * Set the list of GIT credentials.
     */
    public void setGitCreds(List<ExploratoryGitCreds> gitCreds) {
        this.gitCreds = gitCreds;
    }

    /**
     * Set the list of GIT credentials and return this object.
     */
    public T withGitCreds(List<ExploratoryGitCreds> gitCreds) {
        setGitCreds(gitCreds);
        return self;
    }

    /**
     * Set the image name and return this object.
     */
    public T withImageName(String imageName) {
        setImageName(imageName);
        return self;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("gitCreds", gitCreds)
                .add("imageName", imageName);
    }
}
