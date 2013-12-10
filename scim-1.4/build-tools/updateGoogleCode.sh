#!/bin/bash

# Note: CVSDUDE_USER and CVSDUDE_PASSWORD come from environment variables set by Jenkins.

COMMON_UNBOUNDID_OPTS="--username $CVSDUDE_USER --password $CVSDUDE_PASSWORD --non-interactive --no-auth-cache --trust-server-cert"
COMMON_GOOGLE_OPTS="--username $GOOGLE_USER --password $GOOGLE_PASSWORD --non-interactive --no-auth-cache --trust-server-cert"

#Validate the command-line options
if [[ -n "$1" ]]
then
  if [[ "$1" != "--dry-run" ]]
  then
    echo "The only argument allowed is \"--dry-run\". This will do everything except commit the changes back to Google Code."
    exit 1
  fi
fi

rm -rf scimsdk
svn checkout $COMMON_GOOGLE_OPTS https://scimsdk.googlecode.com/svn/trunk/ scimsdk
cd scimsdk

CURRENT_REVISION=`cat .ubid-revision`
LATEST_REVISION=`svn info $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk | grep Revision | grep -o "[0-9]*$"`
echo "Google Code revision is $CURRENT_REVISION."
echo "Latest CVSDude revision is $LATEST_REVISION."
echo

for ((IDX=CURRENT_REVISION+1; IDX <= LATEST_REVISION; IDX++))
do
    STATUS=`svn status | grep -v -E '^(\?)'`
    if [[ -n "$STATUS" ]] && [[ "$1" != "--dry-run" ]]
    then
      echo "SVN status indicates that the workspace is dirty:"
      echo -e "$STATUS"
      exit 1
    fi

    echo "Merging files for revision $IDX..."

    #Turn on command checking to catch any errors with the SVN merge commands
    set -e

    #Handle Build Tools
    svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/build-tools/src build-tools/src

    #Handle Config
    svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/config config

    #Handle Resources
    if [[ -z "$SKIP_RESOURCE_FOLDER" ]]
    then
      svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/resource resource
    fi

    #Handle SCIM-SDK
    svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/scim-sdk/src scim-sdk/src

    #Handle SCIM-LDAP
    svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/scim-ldap/src scim-ldap/src

    #Handle SCIM-RI
    svn merge -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk/scim-ri/src scim-ri/src

    #Turn off command checking
    set +e

    STATUS=`svn status | grep -v -E '^(\?)'`
    if [[ -z "$STATUS" ]]
    then
      #If 'svn status' is empty, there is nothing to commit, so move on to the next revision.
      echo
      echo "Revision $IDX did not modify any files that are checked into Google Code."
      echo
      continue
    else
      #Else, run the build and unit tests.
      echo
      echo "Merged revision $IDX, now running tests..."
      echo
      $M2_HOME/bin/mvn clean verify > maven-output.$IDX.txt
      EXIT_CODE=$?
      if [[ $EXIT_CODE -eq 0 ]]
      then
        echo "All tests passed."
        echo
      else
        echo "The build or unit tests failed. See scimsdk/maven-output.$IDX.txt for more information."
        echo
        if [[ "$1" != "--dry-run" ]]
	    then
          exit $EXIT_CODE
        fi
      fi
    fi

    #If the tests pass, get the commit message for this revision, and use it when committing the changes to Google Code
    LOG_MESSAGE=`svn log -c $IDX $COMMON_UNBOUNDID_OPTS https://svn.unboundid.lab/components/scim/trunk`

    #Update state file (.ubid-revision)
    echo -n $IDX > .ubid-revision

    echo -e "Merged revision $IDX from the UnboundID repository." > commit-message.$IDX.txt
    echo -e "$LOG_MESSAGE" >> commit-message.$IDX.txt
    if [[ "$1" != "--dry-run" ]]
    then
      #Commit the changes back to Google Code
      svn commit -F commit-message.$IDX.txt $COMMON_GOOGLE_OPTS
    fi
done

CURRENT_REVISION=`cat .ubid-revision`
if [[ $CURRENT_REVISION -lt $LATEST_REVISION ]]
then
   echo "Updating .ubid-revision file..."
   echo -n $LATEST_REVISION > .ubid-revision
   svn commit -m "Updating .ubid-revision state file with the latest revision from the UnboundID repository." \
     $COMMON_GOOGLE_OPTS .ubid-revision
fi

echo "Finished."
