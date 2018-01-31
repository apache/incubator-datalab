package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
public class ImageCreateStatusDTO extends StatusBaseDTO<ImageCreateStatusDTO> {

    private ImageCreateDTO imageCreateDTO;
    private String name;
    private String exploratoryName;

    @Data
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    public static class ImageCreateDTO {
        private String externalName;
        private String fullName;
        private String externalId;
        private String user;
        private String application;
        private ImageStatus status;
        private String ip;

        @JsonCreator
        public ImageCreateDTO(@JsonProperty("notebook_image_name") String externalName, @JsonProperty("full_image_name") String fullName,
                              @JsonProperty("image_id") String externalId, @JsonProperty("user_name") String user,
                              @JsonProperty("application") String application, @JsonProperty("status") ImageStatus status,
                              @JsonProperty("ip") String ip) {
            this.externalName = externalName;
            this.fullName = fullName;
            this.externalId = externalId;
            this.user = user;
            this.application = application;
            this.status = status;
            this.ip = ip;
        }
    }
}
