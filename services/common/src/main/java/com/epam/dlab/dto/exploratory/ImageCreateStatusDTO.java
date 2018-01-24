package com.epam.dlab.dto.exploratory;

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ImageCreateStatusDTO extends StatusBaseDTO<ImageCreateStatusDTO> {

    private ImageCreateDTO imageCreateDTO;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageCreateDTO {
        private final String name;
        private final String fullName;
        private final String id;
        private final String user;
        private final String application;
        private final ImageStatus status;
        private final String errorMessage;

        @JsonCreator
        public ImageCreateDTO(@JsonProperty("image_name") String name, @JsonProperty("full_image_name") String fullName,
                              @JsonProperty("image_id") String id, @JsonProperty("user_name") String user,
                              @JsonProperty("application") String application, @JsonProperty("status") ImageStatus status,
                              @JsonProperty("errorMessage") String errorMessage) {
            this.name = name;
            this.fullName = fullName;
            this.id = id;
            this.user = user;
            this.application = application;
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public ImageCreateDTO(String name, String user, ImageStatus status, String errorMessage) {
            this(name, null, null, user, null, status, errorMessage);
        }
    }
}
