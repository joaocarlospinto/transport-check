$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"

# Carrega variaveis do .env
if (Test-Path "$PSScriptRoot\.env") {
    Get-Content "$PSScriptRoot\.env" | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

& "$env:JAVA_HOME\bin\java.exe" `
    "-Dmaven.multiModuleProjectDirectory=$PSScriptRoot" `
    -cp "$PSScriptRoot\.mvn\wrapper\maven-wrapper.jar" `
    org.apache.maven.wrapper.MavenWrapperMain `
    spring-boot:run
