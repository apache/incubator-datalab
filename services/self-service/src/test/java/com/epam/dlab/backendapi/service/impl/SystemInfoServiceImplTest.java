/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.resources.dto.SystemInfoDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystemVersion;
import oshi.software.os.linux.LinuxOperatingSystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SystemInfoServiceImplTest {

	private static final String OS_VERSION = "OS version";
	private static final String OS_FAMILY = "OS FAMILY";
	private static final String BUILD_NUMBER = "BUILD 1.0";
	private static final String PROCESSOR_MODEL = "Proc model";
	private static final long AVAILABLE_MEMORY = 100L;
	@Mock
	private SystemInfo si;

	@InjectMocks
	private SystemInfoServiceImpl systemInfoService;

	@Test
	public void getSystemInfo() {
		final OperatingSystem os = mock(OperatingSystem.class);
		final HardwareAbstractionLayer hardwareAbstractionLayer = mock(HardwareAbstractionLayer.class);
		final OperatingSystemVersion operatingSystemVersion = mock(OperatingSystemVersion.class);
		final CentralProcessor centralProcessor = mock(CentralProcessor.class);
		final GlobalMemory globalMemory = mock(GlobalMemory.class);
		when(operatingSystemVersion.getVersion()).thenReturn(OS_VERSION);
		when(operatingSystemVersion.getBuildNumber()).thenReturn(BUILD_NUMBER);
		when(hardwareAbstractionLayer.getDiskStores()).thenReturn(new HWDiskStore[]{});
		when(os.getFamily()).thenReturn(OS_FAMILY);
		when(os.getVersion()).thenReturn(operatingSystemVersion);
		when(si.getOperatingSystem()).thenReturn(os);
		when(globalMemory.getAvailable()).thenReturn(AVAILABLE_MEMORY);
		when(hardwareAbstractionLayer.getMemory()).thenReturn(globalMemory);
		when(centralProcessor.getModel()).thenReturn(PROCESSOR_MODEL);
		when(hardwareAbstractionLayer.getProcessor()).thenReturn(centralProcessor);

		when(si.getHardware()).thenReturn(hardwareAbstractionLayer);

		SystemInfoDto systemInfo = systemInfoService.getSystemInfo();

		assertEquals(BUILD_NUMBER, systemInfo.getOsInfo().getBuildNumber());
		assertEquals(OS_VERSION, systemInfo.getOsInfo().getVersion());
		assertEquals(OS_FAMILY, systemInfo.getOsInfo().getFamily());
		assertEquals(PROCESSOR_MODEL, systemInfo.getProcessorInfo().getModel());
		assertEquals(AVAILABLE_MEMORY, systemInfo.getMemoryInfo().getAvailableMemory());
		assertTrue(systemInfo.getDisksInfo().isEmpty());

		verify(si).getOperatingSystem();
		verify(si).getHardware();
		verifyNoMoreInteractions(si);
	}

	private OperatingSystem getOs() {
		return new LinuxOperatingSystem();
	}

	private HardwareAbstractionLayer getHal() {
		return new LinuxHardwareAbstractionLayer();
	}
}