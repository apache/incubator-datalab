/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.resources.dto.SystemInfoDto;
import com.epam.datalab.backendapi.service.SystemInfoService;
import com.epam.datalab.model.systeminfo.DiskInfo;
import com.epam.datalab.model.systeminfo.MemoryInfo;
import com.epam.datalab.model.systeminfo.OsInfo;
import com.epam.datalab.model.systeminfo.ProcessorInfo;
import com.google.inject.Inject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SystemInfoServiceImpl implements SystemInfoService {

    @Inject
    private SystemInfo si;

    @Override
    public SystemInfoDto getSystemInfo() {
        HardwareAbstractionLayer hal = si.getHardware();
        final OperatingSystem operatingSystem = si.getOperatingSystem();
        return new SystemInfoDto(getOsInfo(operatingSystem), getProcessorInfo(hal), getMemoryInfo(hal),
                getDiskInfoList(File.listRoots()));
    }

    private OsInfo getOsInfo(OperatingSystem os) {
        return OsInfo.builder()
                .manufacturer(os.getManufacturer())
                .family(os.getFamily())
                .version(os.getVersion().getVersion())
                .buildNumber(os.getVersion().getBuildNumber())
                .build();
    }

    private ProcessorInfo getProcessorInfo(HardwareAbstractionLayer hal) {
        CentralProcessor cp = hal.getProcessor();
        return ProcessorInfo.builder()
                .model(cp.getModel())
                .family(cp.getFamily())
                .name(cp.getName())
                .id(cp.getProcessorID())
                .vendor(cp.getVendor())
                .logicalCoreCount(cp.getLogicalProcessorCount())
                .physicalCoreCount(cp.getPhysicalProcessorCount())
                .isCpu64Bit(cp.isCpu64bit())
                .currentSystemLoad(cp.getSystemCpuLoad())
                .systemLoadAverage(cp.getSystemLoadAverage())
                .build();
    }

    private MemoryInfo getMemoryInfo(HardwareAbstractionLayer hal) {
        GlobalMemory memory = hal.getMemory();
        return MemoryInfo.builder()
                .availableMemory(memory.getAvailable())
                .totalMemory(memory.getTotal())
                .swapTotal(memory.getSwapTotal())
                .swapUsed(memory.getSwapUsed())
                .pagesPageIn(memory.getSwapPagesIn())
                .pagesPageOut(memory.getSwapPagesOut())
                .build();
    }

    private List<DiskInfo> getDiskInfoList(File[] roots) {
        return Arrays.stream(roots).map(this::getDiskInfo).collect(Collectors.toList());
    }

    private DiskInfo getDiskInfo(File fileStore) {
        return DiskInfo.builder()
                .serialNumber(fileStore.getName())
                .usedByteSpace(fileStore.getTotalSpace() - fileStore.getFreeSpace())
                .totalByteSpace(fileStore.getTotalSpace())
                .build();
    }
}
