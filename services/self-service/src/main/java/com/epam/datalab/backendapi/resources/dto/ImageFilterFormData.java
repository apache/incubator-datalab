package com.epam.datalab.backendapi.resources.dto;

import com.epam.datalab.dto.exploratory.ImageSharingStatus;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;


import java.util.Set;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageFilterFormData {
    @NonNull
    private Set<String> imageNames;
    @NonNull
    private Set<ImageStatus> statuses;
    @NonNull
    private Set<String> endpoints;
    @NonNull
    private Set<String> templateNames;
    @NonNull
    private Set<ImageSharingStatus> sharingStatuses;
}
