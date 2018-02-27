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


    public ImageCreateStatusDTO withImageCreateDto(ImageCreateDTO imageCreateDto) {
        setImageCreateDTO(imageCreateDto);
        return this;
    }

    public ImageCreateStatusDTO withoutImageCreateDto() {
        setImageCreateDTO(new ImageCreateDTO());
        return this;
    }

    @Data
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    public static class ImageCreateDTO {
        private String externalName;
        private String fullName;
        private String user;
        private String application;
        private ImageStatus status;
        private String ip;

        @JsonCreator
        public ImageCreateDTO(@JsonProperty("notebook_image_name") String externalName,
                              @JsonProperty("full_image_name") String fullName,
                              @JsonProperty("user_name") String user, @JsonProperty("application") String application,
                              @JsonProperty("status") ImageStatus status, @JsonProperty("ip") String ip) {
            this.externalName = externalName;
            this.fullName = fullName;
            this.user = user;
            this.application = application;
            this.status = status;
            this.ip = ip;
        }
    }
}
