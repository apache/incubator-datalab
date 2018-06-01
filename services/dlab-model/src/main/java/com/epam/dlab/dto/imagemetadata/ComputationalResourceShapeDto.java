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

package com.epam.dlab.dto.imagemetadata;

import com.epam.dlab.util.ObjectUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.Objects;

public class ComputationalResourceShapeDto {
    @JsonProperty("Type")
    private String type;
    @JsonProperty("Size")
    private String size;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("Ram")
    private String ram;
    @JsonProperty("Cpu")
    private int cpu;
    @JsonProperty("Spot")
    private boolean spot = false;

    @JsonProperty("SpotPctPrice")
    private int spotPctPrice = 70;


    public ComputationalResourceShapeDto(){
    }

    public ComputationalResourceShapeDto(String type, String size, String description, String ram, int cpu) {
        this.type = type;
        this.size = size;
        this.description = description;
        this.ram = ram;
        this.cpu = cpu;
    }

    public ComputationalResourceShapeDto(String type, String size, String description, String ram, int cpu, boolean spot, int spotPctPrice) {
        this.type = type;
        this.size = size;
        this.description = description;
        this.ram = ram;
        this.cpu = cpu;
        this.spot = spot;
        this.spotPctPrice = spotPctPrice;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSize() { return size; }

    public void setSize(String size) { this.size = size; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public boolean isSpot() {
        return spot;
    }

    public void setSpot(boolean spot) {
        this.spot = spot;
    }

    public int getSpotPctPrice() {
        return spotPctPrice;
    }

    public void setSpotPctPrice(int spotPctPrice) {
        this.spotPctPrice = spotPctPrice;
    }

    @Override
    public boolean equals(Object obj) {
        return ObjectUtils.areObjectsEqual(this, obj,
                o -> o.type,
                o -> o.size,
                o -> o.description,
                o -> o.ram,
                o -> o.cpu,
                o -> o.spot,
                o -> o.spotPctPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, size, description, ram, cpu, spot, spotPctPrice);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
