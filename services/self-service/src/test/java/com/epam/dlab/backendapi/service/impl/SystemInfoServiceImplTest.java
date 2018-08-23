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
import com.epam.dlab.model.systeminfo.DiskInfo;
import com.epam.dlab.model.systeminfo.MemoryInfo;
import com.epam.dlab.model.systeminfo.OsInfo;
import com.epam.dlab.model.systeminfo.ProcessorInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.platform.linux.LinuxHardwareAbstractionLayer;
import oshi.hardware.platform.mac.MacHardwareAbstractionLayer;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayer;
import oshi.hardware.platform.unix.solaris.SolarisHardwareAbstractionLayer;
import oshi.hardware.platform.windows.WindowsHardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.software.os.mac.MacOperatingSystem;
import oshi.software.os.unix.freebsd.FreeBsdOperatingSystem;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.software.os.windows.WindowsOperatingSystem;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SystemInfoServiceImplTest {

	@Mock
	private SystemInfo si;

	@InjectMocks
	private SystemInfoServiceImpl systemInfoService;

	@Test
	public void getSystemInfo() throws NoSuchFieldException, IllegalAccessException {
		String osName = System.getProperty("os.name");
		OperatingSystem os = getOs(osName);
		HardwareAbstractionLayer hal = getHal(osName);
		when(si.getOperatingSystem()).thenReturn(os);
		when(si.getHardware()).thenReturn(hal);

		OsInfo osInfo = OsInfo.builder()
				.family(osName)
				.buildNumber(System.getProperty("os.version"))
				.build();
		ProcessorInfo processorInfo = ProcessorInfo.builder().build();
		MemoryInfo memoryInfo = MemoryInfo.builder().build();
		DiskInfo diskInfo = DiskInfo.builder().build();
		SystemInfoDto similarObject = new SystemInfoDto(osInfo, processorInfo, memoryInfo,
				Collections.singletonList(diskInfo));

		SystemInfoDto actualObject = systemInfoService.getSystemInfo();

		assertTrue(haveSystemInfoObjectsSimilarOsFields(similarObject, actualObject));

		verify(si).getOperatingSystem();
		verify(si).getHardware();
		verifyNoMoreInteractions(si);
	}

	private OperatingSystem getOs(String osName) {
		if (osName.toLowerCase().contains("windows")) return new WindowsOperatingSystem();
		else if (osName.toLowerCase().contains("linux")) return new LinuxOperatingSystem();
		else if (osName.toLowerCase().contains("freebsd")) return new FreeBsdOperatingSystem();
		else if (osName.toLowerCase().contains("mac")) return new MacOperatingSystem();
		else return new SolarisOperatingSystem();
	}

	private HardwareAbstractionLayer getHal(String osName) {
		if (osName.toLowerCase().contains("windows")) return new WindowsHardwareAbstractionLayer();
		else if (osName.toLowerCase().contains("linux")) return new LinuxHardwareAbstractionLayer();
		else if (osName.toLowerCase().contains("freebsd")) return new FreeBsdHardwareAbstractionLayer();
		else if (osName.toLowerCase().contains("mac")) return new MacHardwareAbstractionLayer();
		else return new SolarisHardwareAbstractionLayer();
	}

	private boolean haveSystemInfoObjectsSimilarOsFields(SystemInfoDto object1, SystemInfoDto object2) throws
			NoSuchFieldException, IllegalAccessException {
		Field osInfo1 = object1.getClass().getDeclaredField("osInfo");
		osInfo1.setAccessible(true);
		OsInfo osInfoObject1 = (OsInfo) osInfo1.get(object1);

		Field osInfo2 = object2.getClass().getDeclaredField("osInfo");
		osInfo2.setAccessible(true);
		OsInfo osInfoObject2 = (OsInfo) osInfo2.get(object2);

		Field family1 = osInfoObject1.getClass().getDeclaredField("family");
		family1.setAccessible(true);

		Field family2 = osInfoObject2.getClass().getDeclaredField("family");
		family2.setAccessible(true);

		Field buildNumber1 = osInfoObject1.getClass().getDeclaredField("buildNumber");
		buildNumber1.setAccessible(true);

		Field buildNumber2 = osInfoObject2.getClass().getDeclaredField("buildNumber");
		buildNumber2.setAccessible(true);

		return family2.get(osInfoObject2).toString().contains(family1.get(osInfoObject1).toString())
				&& buildNumber1.get(osInfoObject1).equals(buildNumber2.get(osInfoObject2));
	}
}