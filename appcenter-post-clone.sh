#!/usr/bin/env bash

# Add the following lines to your appcenter-post-clone.sh script
echo "org.gradle.jvmargs=-Xmx1536m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
echo "org.gradle.console=plain" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
echo "org.gradle.build.scan=true" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
echo "org.gradle.daemon=true" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
echo "org.gradle.parallel=true" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
echo "org.gradle.configureondemand=true" >> $APPCENTER_SOURCE_DIRECTORY/gradle.properties
