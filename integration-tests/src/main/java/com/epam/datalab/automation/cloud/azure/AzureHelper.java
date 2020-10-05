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

package com.epam.datalab.automation.cloud.azure;

import com.epam.datalab.automation.exceptions.CloudException;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AzureHelper{

    private static final Logger LOGGER = LogManager.getLogger(AzureHelper.class);
    private static final Duration CHECK_TIMEOUT = Duration.parse("PT10m");
    private static final String LOCALHOST_IP = ConfigPropertyValue.get("LOCALHOST_IP");

	private static Azure azure = getAzureInstance();

    private AzureHelper(){}

	private static Azure getAzureInstance() {
		if (!ConfigPropertyValue.isRunModeLocal() && Objects.isNull(azure)) {
			try {
				return Azure.configure().authenticate(
						new File(ConfigPropertyValue.getAzureAuthFileName())).withDefaultSubscription();
			} catch (IOException e) {
				LOGGER.info("An exception occured: {}", e);
			}
		}
		return azure;
	}

    private static List<VirtualMachine> getVirtualMachines(){
        return !azure.virtualMachines().list().isEmpty() ? new ArrayList<>(azure.virtualMachines().list()) : null;
    }

    public static List<VirtualMachine> getVirtualMachinesByName(String name, boolean restrictionMode){
        if(ConfigPropertyValue.isRunModeLocal()){

            List<VirtualMachine> vmLocalModeList = new ArrayList<>();
            VirtualMachine mockedVM = mock(VirtualMachine.class);
            PublicIPAddress mockedIPAddress = mock(PublicIPAddress.class);
            NetworkInterface mockedNetworkInterface = mock(NetworkInterface.class);
            when(mockedVM.getPrimaryPublicIPAddress()).thenReturn(mockedIPAddress);
            when(mockedIPAddress.ipAddress()).thenReturn(LOCALHOST_IP);
            when(mockedVM.getPrimaryNetworkInterface()).thenReturn(mockedNetworkInterface);
            when(mockedNetworkInterface.primaryPrivateIP()).thenReturn(LOCALHOST_IP);
            vmLocalModeList.add(mockedVM);

            return vmLocalModeList;

        }
        List<VirtualMachine> vmList = getVirtualMachines();
        if(vmList == null){
            LOGGER.warn("There is not any virtual machine in Azure");
            return vmList;
        }
        if(restrictionMode){
            vmList.removeIf(vm -> !hasName(vm, name));
        }
        else{
            vmList.removeIf(vm -> !containsName(vm, name));
        }
        return !vmList.isEmpty() ? vmList : null;
    }

    private static boolean hasName(VirtualMachine vm, String name){
        return vm.name().equals(name);
    }

    private static boolean containsName(VirtualMachine vm, String name){
        return vm.name().contains(name);
    }

    private static PowerState getStatus(VirtualMachine vm){
        return vm.powerState();
    }

	public static void checkAzureStatus(String virtualMachineName, PowerState expAzureState, boolean restrictionMode)
			throws InterruptedException {
        LOGGER.info("Check status of virtual machine with name {} on Azure", virtualMachineName);
        if (ConfigPropertyValue.isRunModeLocal()) {
            LOGGER.info("Azure virtual machine with name {} fake state is {}", virtualMachineName, expAzureState);
            return;
        }
        List<VirtualMachine> vmsWithName = getVirtualMachinesByName(virtualMachineName, restrictionMode);
        if(vmsWithName == null){
            LOGGER.warn("There is not any virtual machine in Azure with name {}", virtualMachineName);
            return;
        }

        PowerState virtualMachineState;
        long requestTimeout = ConfigPropertyValue.getAzureRequestTimeout().toMillis();
        long timeout = CHECK_TIMEOUT.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;
        VirtualMachine virtualMachine = vmsWithName.get(0);
        while (true) {
            virtualMachineState = getStatus(virtualMachine);
            if (virtualMachineState == expAzureState) {
                break;
            }
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                LOGGER.info("Azure virtual machine with name {} state is {}", virtualMachineName, getStatus(virtualMachine));
                throw new CloudException("Timeout has been expired for check state of azure virtual machine with name " + virtualMachineName);
            }
            Thread.sleep(requestTimeout);
        }

        for (VirtualMachine  vm : vmsWithName) {
            LOGGER.info("Azure virtual machine with name {} state is {}. Virtual machine id {}, private IP {}, public IP {}",
                    virtualMachineName, getStatus(vm), vm.vmId(), vm.getPrimaryNetworkInterface().primaryPrivateIP(),
                    vm.getPrimaryPublicIPAddress() != null ? vm.getPrimaryPublicIPAddress().ipAddress() : "doesn't exist for this resource type");
        }
        Assert.assertEquals(virtualMachineState, expAzureState, "Azure virtual machine with name " + virtualMachineName +
                " state is not correct. Virtual machine id " +
                virtualMachine.vmId() + ", private IP " + virtualMachine.getPrimaryNetworkInterface().primaryPrivateIP() +
                ", public IP " +
                (virtualMachine.getPrimaryPublicIPAddress() != null ? virtualMachine.getPrimaryPublicIPAddress().ipAddress() : "doesn't exist for this resource type" ));
    }

}
