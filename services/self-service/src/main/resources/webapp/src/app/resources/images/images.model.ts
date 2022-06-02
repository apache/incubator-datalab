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
  isShared: boolean;
  status: 'created' | 'creating' | 'terminated' | 'terminating' | 'failed';
  user: string;
  isSelected?: boolean;
}
