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


export class HelpUtils {

  public static getBucketProtocol = (cloud) => {
    switch (cloud) {
      case 'aws':
        return 's3a://';

      case 'gcp':
        return 'gs://';

      case 'azure':
        return 'wasbs://';

      default:
        return;
    }
  }

  public static addSizeToGpuType(index): string {

    const sizes = ['S', 'M', 'L', 'XL', 'XXL'];

    return sizes[index];
  }

  public static sortGpuTypes(gpuType: Array<string> = []): Array<string> {

    const sortedTypes = [
      'nvidia-tesla-t4',
      'nvidia-tesla-k80',
      'nvidia-tesla-p4',
      'nvidia-tesla-p100',
      'nvidia-tesla-v100',
      'nvidia-tesla-a100'
    ];

    return sortedTypes.filter(el => gpuType.includes(el));;
  }
}
