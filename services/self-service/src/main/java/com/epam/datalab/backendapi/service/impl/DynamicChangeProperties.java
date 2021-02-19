package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.modules.ChangePropertiesConst;
import com.epam.datalab.backendapi.modules.RestartForm;
import com.epam.datalab.backendapi.resources.dto.YmlDTO;
import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DynamicChangeProperties implements ChangePropertiesConst {
    private final RESTService externalSelfService;
    private final EndpointDAO endpointDAO;

    @Inject
    public DynamicChangeProperties(RESTService externalSelfService, EndpointDAO endpointDAO) {
        this.externalSelfService = externalSelfService;
        this.endpointDAO = endpointDAO;
    }

    public String getProperties(String path, String name) {
        return readFileAsString(path, name);
    }

    public void overwriteProperties(String path, String name, String ymlString) {
        writeFileFromString(ymlString, name, path);
    }

    public Map<String, String> getPropertiesWithExternal(String endpoint, UserInfo userInfo) {
        EndpointDTO endpointDTO = findEndpointDTO(endpoint);
        Map<String, String> properties = new HashMap<>();
        if (endpoint.equals("local")) {
            properties.put(SELF_SERVICE, getProperties(SELF_SERVICE_PROP_PATH, SELF_SERVICE));
            properties.put(PROVISIONING_SERVICE, getProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE));
            properties.put(BILLING_SERVICE, getProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE));
        } else {
            log.info("Trying to read properties, for external endpoint : {} , for user: {}",
                    endpoint, userInfo.getSimpleName());
            String url = endpointDTO.getUrl() + "/api/config";
            properties.put(SELF_SERVICE,
                    externalSelfService.get(url + "/self-service", userInfo.getAccessToken(), String.class));
            properties.put(PROVISIONING_SERVICE,
                    externalSelfService.get(url + "/provisioning-service", userInfo.getAccessToken(), String.class));
            properties.put(BILLING_SERVICE,
                    externalSelfService.get(url + "/billing", userInfo.getAccessToken(), String.class));
        }
        return properties;
    }

    public void overwritePropertiesWithExternal(String path, String name, YmlDTO ymlDTO, UserInfo userInfo) {
        log.info("Trying to write {}, for external endpoint : {} , for user: {}",
                name, ymlDTO.getEndpointName(), userInfo.getSimpleName());
        EndpointDTO endpoint = findEndpointDTO(ymlDTO.getEndpointName());
        if (ymlDTO.getEndpointName().equals("local")) {
            writeFileFromString(ymlDTO.getYmlString(), name, path);
        } else {
            String url = endpoint.getUrl() + "/api/config/multiple/" + findMethodName(name);
            externalSelfService.post(url, ymlDTO.getYmlString(), userInfo.getAccessToken(), String.class);
        }
    }

    private EndpointDTO findEndpointDTO(String endpointName) {
        return endpointDAO.get(endpointName)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + endpointName
                        + " not found"));
    }

    public void restart(RestartForm restartForm) {
        try {
            boolean billing = restartForm.isBilling();
            boolean provserv = restartForm.isProvserv();
            boolean ui = restartForm.isUi();
            String shCommand = buildSHRestartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {}, provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }


    public void restartForExternal(RestartForm restartForm, UserInfo userInfo) {
        EndpointDTO endpointDTO = findEndpointDTO(restartForm.getEndpoint());
        String url = endpointDTO.getUrl() + "/api/config/multiple/restart";
        log.info("External request for endpoint {}, for user {}", restartForm.getEndpoint(), userInfo.getSimpleName());
        externalSelfService.post(url, userInfo.getAccessToken(), restartForm, Void.class);
    }

    private String findMethodName(String name) {
        switch (name) {
            case "self-service.yml": {
                return "self-service";
            }
            case "provisioning.yml": {
                return "provisioning-service";
            }
            case "billing.yml": {
                return "billing";
            }
            default:
                return "";
        }
    }


    private String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

    private String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.info("Trying to read self-service.yml, file from path {} :", selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }

    private String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher m = Pattern.compile(SECRET_REGEX).matcher(currentConf);
        List<String> secrets = new ArrayList<>();
        String confWithReplacedSecretConf = removeLicence(currentConf);
        while (m.find()) {
            String secret = m.group().split(":")[DEFAULT_VALUE_PLACE];
            if (!(secret.isEmpty() || secret.trim().isEmpty()))
                secrets.add(secret);
        }
        for (String secret : secrets) {
            confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secret, SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private String removeLicence(String conf) {
        return conf.split(LICENCE_REGEX)[conf.split(LICENCE_REGEX).length - 1];
    }

    private void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            changeCHMODE(serviceName, servicePath, DEFAULT_CHMOD, WRITE_CHMOD);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, servicePath, WRITE_CHMOD, DEFAULT_CHMOD);
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
    }

    private void changeCHMODE(String serviceName, String path, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, path);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private String addLicence() {
        return LICENCE;
    }

    private String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, String> emptySecrets = findEmptySecret(newPropFile);
        return emptySecrets.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecrets);
    }

    private String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldProfMatcher = Pattern.compile(SECRET_REGEX).matcher(oldProf);
        while (oldProfMatcher.find()) {
            String[] s = oldProfMatcher.group().split(":");
            if (emptySecrets.containsKey(s[DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[DEFAULT_NAME_PLACE]), oldProfMatcher.group());
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private Map<String, String> findEmptySecret(String newPropFile) {
        Matcher newPropFileMatcher = Pattern.compile(SECRET_REGEX).matcher(newPropFile);
        Map<String, String> emptySecrets = new HashMap<>();
        while (newPropFileMatcher.find()) {
            String[] s = newPropFileMatcher.group().split(":");
            if (s[DEFAULT_VALUE_PLACE].equals(SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[DEFAULT_NAME_PLACE], newPropFileMatcher.group());
            }
        }
        return emptySecrets;
    }
}
