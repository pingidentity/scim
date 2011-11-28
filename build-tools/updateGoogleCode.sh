#!/bin/bash

COMMON_CVSDUDE_OPTS="--username $CVSDUDE_USER --password $CVSDUDE_PASSWORD --non-interactive --no-auth-cache"
COMMON_GOOGLE_OPTS="--username $GOOGLE_USER --password $GOOGLE_PASSWORD --non-interactive --no-auth-cache"

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
svn checkout $COMMON_GOOGLE_OPTS --trust-server-cert https://scimsdk.googlecode.com/svn/trunk/ scimsdk
cd scimsdk

CURRENT_REVISION=`cat .ubid-revision`
LATEST_REVISION=`svn info $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk | grep Revision | grep -o "[0-9]*$"`
echo "Google Code revision is $CURRENT_REVISION."
echo "Latest CVSDude revision is $LATEST_REVISION."
echo

for ((IDX=CURRENT_REVISION+1; IDX <= LATEST_REVISION; IDX++))
do
    STATUS=`svn status`
    if [[ -n "$STATUS" ]] && [[ "$1" != "--dry-run" ]]
    then
      echo "SVN status indicates that the workspace is dirty:"
      echo -e "$STATUS"
      exit 1
    fi

    echo "Merging files for revision $IDX..."

    #Handle Build Tools
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/build-tools/src build-tools/src

    #Handle Config
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/config config

    #Handle Resources
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/resource resource

    #Handle SCIM-Core
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/scim-core/src scim-core/src

    #Handle SCIM-LDAP
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/scim-ldap/src scim-ldap/src

    #Handle SCIM-RI
    svn merge -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk/scim-ri/src scim-ri/src

    STATUS=`svn status`
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
      $M2_HOME/bin/mvn clean integration-test > maven-output.$IDX.txt
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
    LOG_MESSAGE=`svn log -c $IDX $COMMON_CVSDUDE_OPTS https://unboundid-svn.cvsdude.com/components/scim/trunk`

    #Update state file (.ubid-revision)
    echo $LATEST_REVISION > .ubid-revision

    # Commit the changes back to Google Code
    echo -e "Merged revision $IDX from the UnboundID repository." > commit-message.$IDX.txt
    echo -e "$LOG_MESSAGE" >> commit-message.$IDX.txt
    if [[ "$1" != "--dry-run" ]]
	then
      svn commit -F commit-message.$IDX.txt $COMMON_GOOGLE_OPTS
    fi
done
