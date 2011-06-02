#!/bin/sh

# Copyright 2007-2011 UnboundID Corp.
# All Rights Reserved.


# Determine the path to this script.
ORIG_DIR=`pwd`
cd `dirname $0`

SCRIPT_DIR=`pwd`
cd "${ORIG_DIR}"


# Set ANT_HOME to the path of the ant installation.
ANT_HOME="${SCRIPT_DIR}/ext/ant"
export ANT_HOME


# Invoke ant with the default build script.
"${ANT_HOME}/bin/ant" --noconfig ${*}

