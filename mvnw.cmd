@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE (SET "BASE_DIR=%__MVNW_ARG0_NAME__%")
@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%

@REM Strip any trailing backslash so the quoted -Dmaven.multiModuleProjectDirectory
@REM argument below does not end in \" — Windows/Java parse \" as an escaped quote,
@REM which swallows every following argument (including the main class).
@IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" (SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%")

@SET WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
@SET WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
    @IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@IF NOT EXIST "%WRAPPER_JAR%" (
    @IF NOT "%WRAPPER_URL%"=="" (
        powershell -Command "$webclient=New-Object System.Net.WebClient; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR:\=\\%')"
    )
)

@IF NOT "%JAVA_HOME%"=="" (
    @SET JAVA_CMD=%JAVA_HOME%\bin\java.exe
) ELSE (
    @SET JAVA_CMD=java
)

@SET MAVEN_USER_HOME=%USERPROFILE%\.m2
@SET M2=%MAVEN_USER_HOME%

@"%JAVA_CMD%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  "-Dmaven.home=%MAVEN_USER_HOME%\wrapper\dists" ^
  "-Dmaven.wrapper.jar=%WRAPPER_JAR%" ^
  "-Dmaven.wrapper.properties=%WRAPPER_PROPERTIES%" ^
  -cp "%WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
