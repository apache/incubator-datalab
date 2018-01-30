package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
public class ImageCreateStatusDTO extends StatusBaseDTO<ImageCreateStatusDTO> {

    private ImageCreateDTO imageCreateDTO;
    private String name;
    private String exploratoryName;

    @Data
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageCreateDTO {
        private final String externalName;
        private final String fullName;
        private final String externalId;
        private final String user;
        private final String application;
        private final ImageStatus status;
        private final String errorMessage;
        private final String ip;

        @JsonCreator
        public ImageCreateDTO(@JsonProperty("notebook_image_name") String externalName, @JsonProperty("full_image_name") String fullName,
                              @JsonProperty("image_id") String externalId, @JsonProperty("user_name") String user,
                              @JsonProperty("application") String application, @JsonProperty("status") ImageStatus status,
                              @JsonProperty("errorMessage") String errorMessage, @JsonProperty("ip") String ip) {
            this.externalName = externalName;
            this.fullName = fullName;
            this.externalId = externalId;
            this.user = user;
            this.application = application;
            this.status = status;
            this.errorMessage = errorMessage;
            this.ip = ip;
        }

        public ImageCreateDTO(String user, ImageStatus status, String errorMessage) {
            this(null, null, null, user, null, status, errorMessage, null);
        }
    }
}
