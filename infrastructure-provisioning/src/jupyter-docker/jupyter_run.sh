#!/bin/bash

jupyter notebook --config CONF_PATH

if [[$1 == '-d']];then
        while true; do sleep 1000; done
fi

if [[$1 == "-bash"]];then
        /bin/bash
fi