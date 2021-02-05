#!/usr/bin/python3

# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

import sys
import json

if sys.version[0] == '2':
    reload(sys)
    sys.setdefaultencoding('utf8')


if __name__ == "__main__":
    version = None
    try:
        from jupyter_nbformat import reads, write
    except ImportError:
        try:
            # New IPython
            from nbformat import reads, write
        except ImportError:
            # Deprecated since IPython 4.0
            from IPython.nbformat.current import reads, write
            version = 'json'

    input_file = sys.stdin.read()
    if not version:
        data = json.loads(input_file)
        version = data['nbformat']
    data = reads(input_file, version)

    try:
        # IPython
        sheets = data.worksheets
    except AttributeError:
        # Jupyter
        sheets = [data]

    for sheet in sheets:
        for cell in sheet.cells:
            # Uncomment next 2 lines and comment next section to clear all output in notebook
            # if "outputs" in cell:
            #     cell.outputs = []

            if hasattr(cell, "outputs") and len(cell.outputs) >= 1:
                for field in cell.outputs[0]:
                    if field == "execution_count":
                        cell.outputs[0].execution_count = None
                    elif field == "metadata":
                        cell.outputs[0].metadata = dict()

            for field in ("execution_count",):
                if field in cell:
                    cell[field] = None
            for field in ("prompt_number", "execution_number"):
                if field in cell:
                    del cell[field]

            if "metadata" in cell:
                for field in ("collapsed", "scrolled", "ExecuteTime"):
                    if field in cell.metadata:
                        del cell.metadata[field]

        if hasattr(sheet.metadata, "widgets"):
            del sheet.metadata["widgets"]

        if hasattr(sheet.metadata.language_info, "version"):
            del sheet.metadata.language_info["version"]

    if 'signature' in data.metadata:
        data.metadata['signature'] = ""

    write(data, sys.stdout, version)