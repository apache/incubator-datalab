export interface ProjectModel {
  project: string;
  images: ImageModel[];
}

export interface ImageModel {
  application: string;
  cloudProvider: 'AWS' | 'GCP' | 'Azure';
  timestamp: string;
  description: string;
  endpoint: string;
  fullName: string;
  instanceName: string;
  name: string;
  project: string;
  shared: boolean;
  status: 'created' | 'creating' | 'terminated' | 'terminating' | 'failed';
  user: string;
  isSelected?: boolean;
  libraries: Library[];
  computationalLibraries: Library[];
  clusterConfig: ClusterConfig;
}

export interface ShareImageAllUsersParams {
  imageName: string;
  projectName: string;
  endpoint: string;
}

export interface ModalData {
  image: ImageModel;
}

export interface Library {
  add_pkgs: string[];
  available_versions: string[];
  error_message: string;
  group: string;
  name: string;
  status: string;
  version: string;
}

export interface ClusterConfig {
  Classification: string;
  Properties: Record<string, any>;
  Configurations: any[];
}
